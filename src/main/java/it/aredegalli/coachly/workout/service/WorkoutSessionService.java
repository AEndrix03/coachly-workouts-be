package it.aredegalli.coachly.workout.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.aredegalli.coachly.workout.controller.request.WorkoutSessionSyncRequest;
import it.aredegalli.coachly.workout.dto.snapshot.SessionSnapshotDto;
import it.aredegalli.coachly.workout.dto.snapshot.SnapshotBlockDto;
import it.aredegalli.coachly.workout.dto.snapshot.SnapshotEntryDto;
import it.aredegalli.coachly.workout.dto.snapshot.SnapshotSetDto;
import it.aredegalli.coachly.workout.enums.LoadUnit;
import it.aredegalli.coachly.workout.enums.SessionStatus;
import it.aredegalli.coachly.workout.enums.SetType;
import it.aredegalli.coachly.workout.model.WorkoutSession;
import it.aredegalli.coachly.workout.repository.WorkoutRepository;
import it.aredegalli.coachly.workout.repository.WorkoutSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class WorkoutSessionService {

    private final WorkoutSessionRepository workoutSessionRepository;
    private final WorkoutRepository workoutRepository;
    private final SnapshotAnalyzer snapshotAnalyzer;
    private final ObjectMapper objectMapper;

    public WorkoutSessionService(
        WorkoutSessionRepository workoutSessionRepository,
        WorkoutRepository workoutRepository,
        SnapshotAnalyzer snapshotAnalyzer,
        ObjectMapper objectMapper
    ) {
        this.workoutSessionRepository = workoutSessionRepository;
        this.workoutRepository = workoutRepository;
        this.snapshotAnalyzer = snapshotAnalyzer;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void syncWorkoutSession(UUID userId, UUID workoutId, WorkoutSessionSyncRequest request) {
        workoutRepository.findByIdAndUserId(workoutId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));

        UUID sessionId = request.getClientSessionId() == null
            ? UUID.randomUUID()
            : request.getClientSessionId();

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startedAt = request.getStartedAt() == null ? now : request.getStartedAt();
        OffsetDateTime completedAt = request.getCompletedAt();

        SessionSnapshotDto snapshot = toSnapshot(request);
        SnapshotAnalyzer.SessionStats stats = snapshotAnalyzer.extract(snapshot);

        WorkoutSession workoutSession = workoutSessionRepository.findByIdAndUserId(sessionId, userId)
            .orElseGet(() -> WorkoutSession.builder().id(sessionId).userId(userId).build());

        workoutSession.setWorkoutId(workoutId);
        workoutSession.setStatus(completedAt == null ? SessionStatus.IN_PROGRESS : SessionStatus.COMPLETED);
        workoutSession.setStartedAt(startedAt);
        workoutSession.setEndedAt(completedAt);
        workoutSession.setDurationSeconds(computeDurationSeconds(startedAt, completedAt));
        workoutSession.setSnapshot(serializeSnapshot(snapshot));
        workoutSession.setTotalSets(stats.totalSets());
        workoutSession.setCompletedSets(stats.completedSets());
        workoutSession.setTotalVolumeKg(stats.totalVolumeKg());
        workoutSession.setNotes(request.getNotes());
        workoutSession.setSyncedAt(now);

        workoutSessionRepository.save(workoutSession);
    }

    private Integer computeDurationSeconds(OffsetDateTime startedAt, OffsetDateTime completedAt) {
        if (startedAt == null || completedAt == null || completedAt.isBefore(startedAt)) {
            return null;
        }
        long seconds = completedAt.toEpochSecond() - startedAt.toEpochSecond();
        return (int) seconds;
    }

    private String serializeSnapshot(SessionSnapshotDto snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid workout session snapshot", exception);
        }
    }

    private SessionSnapshotDto toSnapshot(WorkoutSessionSyncRequest request) {
        SessionSnapshotDto snapshot = new SessionSnapshotDto();
        SnapshotBlockDto block = new SnapshotBlockDto();
        block.setPosition(0);
        block.setEntries(toSnapshotEntries(request.getEntries()));

        snapshot.setBlocks(List.of(block));
        return snapshot;
    }

    private List<SnapshotEntryDto> toSnapshotEntries(List<WorkoutSessionSyncRequest.Entry> rawEntries) {
        if (rawEntries == null || rawEntries.isEmpty()) {
            return List.of();
        }

        List<WorkoutSessionSyncRequest.Entry> entries = new ArrayList<>(rawEntries);
        entries.sort(Comparator.comparing(
            entry -> entry.getPosition() == null ? Integer.MAX_VALUE : entry.getPosition()
        ));

        List<SnapshotEntryDto> mappedEntries = new ArrayList<>(entries.size());
        for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
            WorkoutSessionSyncRequest.Entry entry = entries.get(entryIndex);
            SnapshotEntryDto snapshotEntry = new SnapshotEntryDto();
            snapshotEntry.setExerciseId(entry.getExerciseId());
            snapshotEntry.setPosition(entry.getPosition() == null ? entryIndex : entry.getPosition());
            snapshotEntry.setSets(toSnapshotSets(entry));
            mappedEntries.add(snapshotEntry);
        }

        return mappedEntries;
    }

    private List<SnapshotSetDto> toSnapshotSets(WorkoutSessionSyncRequest.Entry entry) {
        List<WorkoutSessionSyncRequest.SetRow> rawSets = entry.getSets();
        if (rawSets == null || rawSets.isEmpty()) {
            return List.of();
        }

        List<WorkoutSessionSyncRequest.SetRow> sets = new ArrayList<>(rawSets);
        sets.sort(Comparator.comparing(
            set -> set.getPosition() == null ? Integer.MAX_VALUE : set.getPosition()
        ));

        List<SnapshotSetDto> mappedSets = new ArrayList<>(sets.size());
        for (int setIndex = 0; setIndex < sets.size(); setIndex++) {
            WorkoutSessionSyncRequest.SetRow set = sets.get(setIndex);
            SnapshotSetDto snapshotSet = new SnapshotSetDto();
            snapshotSet.setPosition(set.getPosition() == null ? setIndex : set.getPosition());
            snapshotSet.setSetType(set.getSetType() == null ? SetType.NORMAL : set.getSetType());
            snapshotSet.setPlannedReps(set.getReps());
            snapshotSet.setActualReps(set.getReps());
            snapshotSet.setPlannedLoad(set.getLoad());
            snapshotSet.setActualLoad(set.getLoad());
            snapshotSet.setLoadUnit(set.getLoadUnit() == null ? LoadUnit.KG : set.getLoadUnit());

            boolean completed = set.getCompleted() != null
                ? set.getCompleted()
                : Boolean.TRUE.equals(entry.getCompleted());
            snapshotSet.setCompleted(completed);
            snapshotSet.setSkipped(!completed);
            snapshotSet.setNotes(set.getNotes());
            mappedSets.add(snapshotSet);
        }

        return mappedSets;
    }
}
