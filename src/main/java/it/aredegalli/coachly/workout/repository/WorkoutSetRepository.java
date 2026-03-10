package it.aredegalli.coachly.workout.repository;

import it.aredegalli.coachly.workout.model.WorkoutSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, UUID> {

    List<WorkoutSet> findAllByEntry_IdOrderByPositionAsc(UUID entryId);
}
