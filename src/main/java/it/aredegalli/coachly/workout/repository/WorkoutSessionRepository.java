package it.aredegalli.coachly.workout.repository;

import it.aredegalli.coachly.workout.enums.SessionStatus;
import it.aredegalli.coachly.workout.model.WorkoutSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {

    Optional<WorkoutSession> findByIdAndUserId(UUID id, UUID userId);

    List<WorkoutSession> findAllByUserIdOrderByStartedAtDesc(UUID userId);

    List<WorkoutSession> findAllByUserIdAndStatusOrderByStartedAtDesc(UUID userId, SessionStatus status);
}
