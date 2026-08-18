package it.aredegalli.coachly.workout.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.aredegalli.coachly.workout.dto.command.WorkoutBlockEntryUpsertRequestDto;
import it.aredegalli.coachly.workout.dto.command.WorkoutBlockUpsertRequestDto;
import it.aredegalli.coachly.workout.dto.WorkoutDto;
import it.aredegalli.coachly.workout.dto.command.WorkoutSetUpsertRequestDto;
import it.aredegalli.coachly.workout.dto.command.WorkoutTranslationDto;
import it.aredegalli.coachly.workout.dto.command.WorkoutUpsertRequestDto;
import it.aredegalli.coachly.workout.enums.LoadUnit;
import it.aredegalli.coachly.workout.enums.IntensityType;
import it.aredegalli.coachly.workout.enums.SetType;
import it.aredegalli.coachly.workout.enums.WorkoutStatus;
import it.aredegalli.coachly.workout.enums.WorkoutGroupType;
import it.aredegalli.coachly.workout.mapper.WorkoutMapper;
import it.aredegalli.coachly.workout.model.Workout;
import it.aredegalli.coachly.workout.model.WorkoutBlock;
import it.aredegalli.coachly.workout.model.WorkoutBlockEntry;
import it.aredegalli.coachly.workout.model.WorkoutSet;
import it.aredegalli.coachly.workout.repository.WorkoutRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;

@Service
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final WorkoutMapper workoutMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorkoutService(WorkoutRepository workoutRepository, WorkoutMapper workoutMapper) {
        this.workoutRepository = workoutRepository;
        this.workoutMapper = workoutMapper;
    }

    @Transactional(readOnly = true)
    public List<WorkoutDto> getUserWorkouts(UUID userId) {
        return workoutMapper.toDtoList(workoutRepository.findAllByUserIdOrderByUpdatedAtDesc(userId));
    }

    @Transactional(readOnly = true)
    public WorkoutDto getUserWorkout(UUID userId, UUID workoutId) {
        Workout workout = workoutRepository.findByIdAndUserId(workoutId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));
        return workoutMapper.toDto(workout);
    }

    @Transactional
    public WorkoutDto createWorkout(UUID userId, WorkoutUpsertRequestDto request) {
        UUID workoutId = request.getId() == null ? UUID.randomUUID() : request.getId();
        if (request.getId() != null
            && workoutRepository.findByIdAndUserId(workoutId, userId).isEmpty()
            && workoutRepository.existsById(workoutId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found");
        }

        Workout workout = toEntity(request, userId, workoutId);
        return workoutMapper.toDto(workoutRepository.save(workout));
    }

    @Transactional
    public WorkoutDto updateWorkout(UUID userId, UUID workoutId, WorkoutUpsertRequestDto request) {
        Workout workout = workoutRepository.findByIdAndUserId(workoutId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));

        // Remove existing nested structure first to avoid unique constraint clashes
        // on (workout_id, position) while replacing blocks in the same transaction.
        workout.getBlocks().clear();
        workoutRepository.flush();

        applyRequestToWorkout(workout, request, userId, workoutId);
        return workoutMapper.toDto(workoutRepository.save(workout));
    }

    private Workout toEntity(WorkoutUpsertRequestDto request, UUID userId, UUID workoutId) {
        Workout workout = Workout.builder()
            .id(workoutId)
            .userId(userId)
            .blocks(new ArrayList<>())
            .build();
        applyRequestToWorkout(workout, request, userId, workoutId);
        return workout;
    }

    private void applyRequestToWorkout(
        Workout workout,
        WorkoutUpsertRequestDto request,
        UUID userId,
        UUID workoutId
    ) {
        workout.setId(workoutId);
        workout.setUserId(userId);
        workout.setName(request.getName().trim());
        workout.setTranslations(serializeTranslations(request.getTranslations()));
        workout.setStatus(request.getStatus() == null ? WorkoutStatus.ACTIVE : request.getStatus());

        List<WorkoutBlockUpsertRequestDto> requestedBlocks = request.getBlocks() == null ? List.of() : request.getBlocks();
        for (int blockIndex = 0; blockIndex < requestedBlocks.size(); blockIndex++) {
            WorkoutBlock block = toBlockEntity(requestedBlocks.get(blockIndex), blockIndex, workout);
            workout.getBlocks().add(block);
        }
    }

    private WorkoutBlock toBlockEntity(WorkoutBlockUpsertRequestDto request, int blockIndex, Workout workout) {
        validateBlockRequest(request);
        WorkoutBlock block = WorkoutBlock.builder()
            .id(request.getId() == null ? UUID.randomUUID() : request.getId())
            .workout(workout)
            .position(positionOrIndex(request.getPosition(), blockIndex))
            .label(request.getLabel())
            .sectionId(request.getSectionId())
            .sectionPosition(request.getSectionPosition())
            .sectionTitle(request.getSectionTitle())
            .sectionKind(request.getSectionKind())
            .groupType(request.getGroupType())
            .rounds(request.getRounds())
            .restBetweenExercisesSeconds(request.getRestBetweenExercisesSeconds())
            .restSeconds(request.getRestSeconds())
            .notes(request.getNotes())
            .entries(new ArrayList<>())
            .build();

        List<WorkoutBlockEntryUpsertRequestDto> requestedEntries = request.getEntries() == null ? List.of() : request.getEntries();
        for (int entryIndex = 0; entryIndex < requestedEntries.size(); entryIndex++) {
            WorkoutBlockEntry entry = toEntryEntity(requestedEntries.get(entryIndex), entryIndex, block);
            block.getEntries().add(entry);
        }

        return block;
    }

    private void validateBlockRequest(WorkoutBlockUpsertRequestDto request) {
        List<WorkoutBlockEntryUpsertRequestDto> entries = request.getEntries() == null
            ? List.of()
            : request.getEntries();
        WorkoutGroupType type = request.getGroupType();

        if (type == WorkoutGroupType.EXERCISE && entries.size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "exercise blocks require exactly one entry");
        }
        if ((type == WorkoutGroupType.SUPERSET || type == WorkoutGroupType.CIRCUIT) && entries.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "exercise groups require at least two entries");
        }

        HashSet<UUID> instanceIds = new HashSet<>();
        for (WorkoutBlockEntryUpsertRequestDto entry : entries) {
            if (entry.getId() != null && !instanceIds.add(entry.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "an exercise instance cannot appear twice in a group");
            }
        }
    }

    private WorkoutBlockEntry toEntryEntity(WorkoutBlockEntryUpsertRequestDto request, int entryIndex, WorkoutBlock block) {
        WorkoutBlockEntry entry = WorkoutBlockEntry.builder()
            .id(request.getId() == null ? UUID.randomUUID() : request.getId())
            .block(block)
            .exerciseId(request.getExerciseId())
            .position(positionOrIndex(request.getPosition(), entryIndex))
            .sets(new ArrayList<>())
            .build();

        List<WorkoutSetUpsertRequestDto> requestedSets = request.getSets() == null ? List.of() : request.getSets();
        for (int setIndex = 0; setIndex < requestedSets.size(); setIndex++) {
            WorkoutSet set = toSetEntity(requestedSets.get(setIndex), setIndex, entry);
            entry.getSets().add(set);
        }

        return entry;
    }

    private WorkoutSet toSetEntity(WorkoutSetUpsertRequestDto request, int setIndex, WorkoutBlockEntry entry) {
        validateSetRequest(request);
        return WorkoutSet.builder()
            .id(request.getId() == null ? UUID.randomUUID() : request.getId())
            .entry(entry)
            .position(positionOrIndex(request.getPosition(), setIndex))
            .setType(request.getSetType() == null ? SetType.NORMAL : request.getSetType())
            .reps(request.getReps())
            .repsMin(request.getRepsMin())
            .repsMax(request.getRepsMax())
            .intensityType(request.getIntensityType() == null ? IntensityType.NONE : request.getIntensityType())
            .intensityMin(request.getIntensityMin())
            .intensityMax(request.getIntensityMax())
            .relativeLoadPercent(request.getRelativeLoadPercent())
            .load(request.getLoad())
            .loadUnit(request.getLoadUnit() == null ? LoadUnit.KG : request.getLoadUnit())
            .restSeconds(request.getRestSeconds())
            .tempo(request.getTempo())
            .pauseSeconds(request.getPauseSeconds())
            .unilateral(request.getUnilateral())
            .notes(request.getNotes())
            .build();
    }

    private void validateSetRequest(WorkoutSetUpsertRequestDto request) {
        if (request.getRepsMin() != null && request.getRepsMax() != null
            && request.getRepsMin() > request.getRepsMax()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "repsMin cannot be greater than repsMax");
        }
        if (request.getIntensityMin() != null && request.getIntensityMax() != null
            && request.getIntensityMin().compareTo(request.getIntensityMax()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "intensityMin cannot be greater than intensityMax");
        }

        IntensityType type = request.getIntensityType() == null ? IntensityType.NONE : request.getIntensityType();
        if (type == IntensityType.NONE
            && (request.getIntensityMin() != null || request.getIntensityMax() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "intensity values require an intensityType");
        }
    }

    private short positionOrIndex(Short value, int index) {
        return value == null ? (short) index : value;
    }

    private String serializeTranslations(Map<String, WorkoutTranslationDto> translations) {
        Map<String, WorkoutTranslationDto> safeTranslations = translations == null ? new LinkedHashMap<>() : translations;
        try {
            return objectMapper.writeValueAsString(safeTranslations);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid workout translations payload", ex);
        }
    }
}
