package it.aredegalli.coachly.workout.repository;

import it.aredegalli.coachly.workout.model.Workout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutRepository extends JpaRepository<Workout, UUID> {

    Optional<Workout> findByIdAndUserId(UUID id, UUID userId);

    List<Workout> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);
}
