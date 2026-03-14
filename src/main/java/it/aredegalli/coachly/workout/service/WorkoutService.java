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
import it.aredegalli.coachly.workout.enums.SetType;
import it.aredegalli.coachly.workout.enums.WorkoutStatus;
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
        workoutRepository.findByIdAndUserId(workoutId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));

        Workout workout = toEntity(request, userId, workoutId);
        return workoutMapper.toDto(workoutRepository.save(workout));
    }

    private Workout toEntity(WorkoutUpsertRequestDto request, UUID userId, UUID workoutId) {
        Workout workout = Workout.builder()
            .id(workoutId)
            .userId(userId)
            .name(request.getName().trim())
            .translations(serializeTranslations(request.getTranslations()))
            .status(request.getStatus() == null ? WorkoutStatus.ACTIVE : request.getStatus())
            .blocks(new ArrayList<>())
            .build();

        List<WorkoutBlockUpsertRequestDto> requestedBlocks = request.getBlocks() == null ? List.of() : request.getBlocks();
        for (int blockIndex = 0; blockIndex < requestedBlocks.size(); blockIndex++) {
            WorkoutBlock block = toBlockEntity(requestedBlocks.get(blockIndex), blockIndex, workout);
            workout.getBlocks().add(block);
        }

        return workout;
    }

    private WorkoutBlock toBlockEntity(WorkoutBlockUpsertRequestDto request, int blockIndex, Workout workout) {
        WorkoutBlock block = WorkoutBlock.builder()
            .id(request.getId() == null ? UUID.randomUUID() : request.getId())
            .workout(workout)
            .position(positionOrIndex(request.getPosition(), blockIndex))
            .label(request.getLabel())
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
        return WorkoutSet.builder()
            .id(request.getId() == null ? UUID.randomUUID() : request.getId())
            .entry(entry)
            .position(positionOrIndex(request.getPosition(), setIndex))
            .setType(request.getSetType() == null ? SetType.NORMAL : request.getSetType())
            .reps(request.getReps())
            .load(request.getLoad())
            .loadUnit(request.getLoadUnit() == null ? LoadUnit.KG : request.getLoadUnit())
            .restSeconds(request.getRestSeconds())
            .notes(request.getNotes())
            .build();
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
