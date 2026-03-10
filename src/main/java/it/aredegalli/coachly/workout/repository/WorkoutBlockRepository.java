package it.aredegalli.coachly.workout.repository;

import it.aredegalli.coachly.workout.model.WorkoutBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkoutBlockRepository extends JpaRepository<WorkoutBlock, UUID> {

    List<WorkoutBlock> findAllByWorkout_IdOrderByPositionAsc(UUID workoutId);
}
