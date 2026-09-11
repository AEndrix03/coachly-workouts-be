package it.aredegalli.coachly.workout.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Un fatto accaduto durante una sessione di allenamento.
 *
 * <p>La tabella e' <strong>append-only</strong>: nessun {@code UPDATE},
 * nessun {@code DELETE}. Lo stato corrente di una sessione e' una proiezione
 * di questi eventi, non il contrario.
 *
 * <p>L'idempotenza sta nel vincolo unico {@code (session_id, seq)}: un batch
 * ritentato dopo un timeout ambiguo non duplica nulla, e il client puo'
 * reinviare senza chiedersi se la prima volta era arrivata.
 */
@Entity
@Table(schema = "workout", name = "workout_session_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutSessionEvent {

    /** Generato dal client: chiave di idempotenza della singola riga. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Riferimento opaco alla sessione, senza FK: gli eventi possono arrivare
     * prima della sessione, perche' le due code dell'outbox non salgono
     * necessariamente nello stesso ordine.
     */
    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** Ordine per sessione, assegnato dal client. */
    @Column(name = "seq", nullable = false, updatable = false)
    private Integer seq;

    /** Quando e' accaduto sul dispositivo, non quando e' arrivato. */
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "type", nullable = false, length = 64, updatable = false)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "received_at", nullable = false, updatable = false)
    private OffsetDateTime receivedAt;
}
