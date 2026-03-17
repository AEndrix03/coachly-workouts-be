package it.aredegalli.coachly.workout.model;

import it.aredegalli.coachly.workout.enums.SessionStatus;
import it.aredegalli.coachly.workout.model.converter.SessionStatusConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The execution of a workout — saved as a complete snapshot object.
 *
 * <h3>Offline-first sync model</h3>
 * <p>This entity is <strong>never created server-side</strong>. It is built
 * entirely on the client while the user trains, then synced to the BE as a
 * complete object via {@code PUT /api/sessions/{id}} (upsert).
 *
 * <p>The client generates the UUID before the session starts, ensuring the
 * entity identity is stable even if the device goes offline mid-workout.
 * Sending the same session multiple times is safe — the upsert is idempotent,
 * using {@code synced_at} as a last-write-wins guard.
 *
 * <h3>Snapshot JSONB structure</h3>
 * <pre>{@code
 * {
 *   "workout_name": "Forza Upper Body",
 *   "blocks": [
 *     {
 *       "source_block_id": "uuid|null",    // null = added live during session
 *       "label": "A",
 *       "position": 0,
 *       "rest_seconds": 120,
 *       "entries": [
 *         {
 *           "source_entry_id": "uuid|null",
 *           "exercise_id": "uuid",
 *           "exercise_name": "Panca Piana", // denormalized — stable across catalog changes
 *           "position": 0,
 *           "sets": [
 *             {
 *               "source_set_id": "uuid|null",
 *               "position": 0,
 *               "set_type": "normal",
 *               "planned_reps": 5,
 *               "planned_load": 100.0,
 *               "load_unit": "kg",
 *               "actual_reps": 5,
 *               "actual_load": 102.5,
 *               "completed": true,
 *               "skipped": false,
 *               "rest_seconds": 180,
 *               "notes": "ottima serie"
 *             }
 *           ]
 *         }
 *       ]
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <p>{@code source_*_id = null} means the element was added by the user
 * during the session (not copied from the workout template).
 *
 * <h3>Denormalized stats</h3>
 * <p>{@link #totalSets}, {@link #completedSets} and {@link #totalVolumeKg}
 * are extracted from the snapshot by {@code SnapshotAnalyzer} at sync time.
 * This avoids JSONB scans on every stats or dashboard query.
 */
@Entity
@Table(schema = "workout", name = "workout_session")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutSession {

    /**
     * Client-generated UUID. Created on the device before the session starts.
     * Functions as the idempotency key for upsert on sync.
     */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Opaque external reference to user-profile-be.
     * Populated from the {@code X-User-Id} header injected by the gateway.
     */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /**
     * Source workout template UUID. Null for free/unplanned sessions.
     * Opaque reference — no DB-level FK to allow sessions to outlive
     * deleted workout templates.
     */
    @Column(name = "workout_id")
    private UUID workoutId;

    @Convert(converter = SessionStatusConverter.class)
    @ColumnTransformer(write = "?::workout.session_status")
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    /**
     * Session start time as recorded by the client.
     * Not the server receipt time — critical for correct offline timestamps.
     */
    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    /**
     * Session end time. Set by the client on complete or abandon.
     * Null while the session is {@code in_progress}.
     */
    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    /**
     * Total active workout duration in seconds, computed by the client.
     * May differ from {@code endedAt - startedAt} if the user paused the timer.
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * Full denormalized session structure including all blocks, entries,
     * planned values, actual values and completion flags.
     * See class Javadoc for the JSON schema.
     *
     * <p>Deserialized to {@code SessionSnapshotDto} at the service layer.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot", nullable = false, columnDefinition = "jsonb")
    private String snapshot;

    // ── Denormalized stats (extracted from snapshot at sync time) ────────

    /**
     * Total number of non-skipped sets in the session.
     * Extracted from snapshot by {@code SnapshotAnalyzer} at sync time.
     * Used for quick progress queries without scanning JSONB.
     */
    @Column(name = "total_sets", nullable = false)
    private Short totalSets;

    /**
     * Number of sets marked as completed.
     * Extracted from snapshot by {@code SnapshotAnalyzer} at sync time.
     */
    @Column(name = "completed_sets", nullable = false)
    private Short completedSets;

    /**
     * Total training volume in kg: sum of {@code actual_reps × actual_load}
     * converted to kg across all completed sets.
     * Null if no load-based sets were completed (e.g. bodyweight-only session).
     */
    @Column(name = "total_volume_kg", precision = 10, scale = 2)
    private BigDecimal totalVolumeKg;

    /** Free-text notes about the overall session. */
    @Column(name = "notes")
    private String notes;

    /**
     * Timestamp of the last successful sync from the client.
     * Used as last-write-wins guard: if an incoming sync has a
     * {@code synced_at} older than the stored value, it is ignored.
     */
    @Column(name = "synced_at", nullable = false)
    private OffsetDateTime syncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ── Lifecycle hooks ──────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.syncedAt == null) {
            this.syncedAt = now;
        }
        if (this.status == null) {
            this.status = SessionStatus.IN_PROGRESS;
        }
        if (this.snapshot == null) {
            this.snapshot = "{}";
        }
        if (this.totalSets == null) {
            this.totalSets = 0;
        }
        if (this.completedSets == null) {
            this.completedSets = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
