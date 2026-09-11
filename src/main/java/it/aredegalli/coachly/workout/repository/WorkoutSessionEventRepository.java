package it.aredegalli.coachly.workout.repository;

import it.aredegalli.coachly.workout.model.WorkoutSessionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface WorkoutSessionEventRepository extends JpaRepository<WorkoutSessionEvent, UUID> {

    /**
     * Append idempotente di un singolo evento.
     *
     * <p>E' una query nativa e non un {@code save} perche' l'idempotenza deve
     * stare nel database, non in una lettura preventiva: fra un
     * {@code SELECT} di controllo e l'{@code INSERT} ci sta un reinvio
     * concorrente dello stesso batch, e il vincolo unico sarebbe l'unica cosa
     * a fermarlo — con un'eccezione invece che con un no-op.
     *
     * @return 1 se l'evento e' stato inserito, 0 se c'era gia'
     */
    @Modifying
    @Query(value = """
        INSERT INTO workout.workout_session_event
            (id, session_id, user_id, seq, occurred_at, type, payload, received_at)
        VALUES
            (:id, :sessionId, :userId, :seq, :occurredAt, :type, CAST(:payload AS jsonb), :receivedAt)
        ON CONFLICT (session_id, seq) DO NOTHING
        """, nativeQuery = true)
    int appendIfAbsent(
        @Param("id") UUID id,
        @Param("sessionId") UUID sessionId,
        @Param("userId") UUID userId,
        @Param("seq") Integer seq,
        @Param("occurredAt") OffsetDateTime occurredAt,
        @Param("type") String type,
        @Param("payload") String payload,
        @Param("receivedAt") OffsetDateTime receivedAt
    );

    long countBySessionId(UUID sessionId);
}
