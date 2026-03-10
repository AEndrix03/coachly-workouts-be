package it.aredegalli.coachly.workout.repository;

import it.aredegalli.coachly.workout.model.WorkoutBlockEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkoutBlockEntryRepository extends JpaRepository<WorkoutBlockEntry, UUID> {

    List<WorkoutBlockEntry> findAllByBlock_IdOrderByPositionAsc(UUID blockId);
}
