package it.aredegalli.coachly.workout.model;

import it.aredegalli.coachly.workout.enums.LoadUnit;
import it.aredegalli.coachly.workout.enums.SetType;
import it.aredegalli.coachly.workout.model.converter.LoadUnitConverter;
import it.aredegalli.coachly.workout.model.converter.SetTypeConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single planned set row within a {@link WorkoutBlockEntry}.
 *
 * <p>This represents one "line" in the workout template UI,
 * e.g. "Set 1 — 5 reps @ 100 kg — normal".
 *
 * <p><strong>Rest priority:</strong>
 * <ol>
 *   <li>{@link #restSeconds} on this set (if not null) — highest priority</li>
 *   <li>{@link WorkoutBlock#restSeconds} — block-level default</li>
 *   <li>No rest prescribed — lowest priority</li>
 * </ol>
 *
 * <p>{@link #reps} is nullable to support open/AMRAP sets.
 * When null, use {@code set_type = amrap} to signal intent.
 *
 * <p>Client-generated UUID — see {@link Workout} for offline-first rationale.
 */
@Entity
@Table(
        schema = "workout",
        name = "workout_set",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_workout_set_position",
                columnNames = {"entry_id", "position"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutSet {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Parent block entry. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private WorkoutBlockEntry entry;

    /**
     * 0-based set number within the entry.
     * Managed by the client — server stores whatever is sent.
     */
    @Column(name = "position", nullable = false)
    private Short position;

    /**
     * Execution type for this set.
     * Defaults to {@link SetType#NORMAL}.
     */
    @Convert(converter = SetTypeConverter.class)
    @Column(name = "set_type", nullable = false, length = 20)
    private SetType setType;

    /**
     * Planned repetitions. Null means open/AMRAP — combine with
     * {@code set_type = amrap} to express intent clearly.
     */
    @Column(name = "reps")
    private Short reps;

    /**
     * Planned load. Null for bodyweight/band/unloaded exercises.
     * Unit is defined by {@link #loadUnit}.
     */
    @Column(name = "load", precision = 6, scale = 2)
    private BigDecimal load;

    /**
     * Unit of the planned load value.
     * For {@code bodyweight}, {@code band} and {@code machine_notch}
     * the load value is typically null or used as a reference level.
     */
    @Convert(converter = LoadUnitConverter.class)
    @Column(name = "load_unit", nullable = false, length = 20)
    private LoadUnit loadUnit;

    /**
     * Rest in seconds after this specific set.
     * When set, takes priority over {@link WorkoutBlock#restSeconds}.
     * Null means the block-level rest applies.
     */
    @Column(name = "rest_seconds")
    private Short restSeconds;

    @Column(name = "notes")
    private String notes;

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
        if (this.setType == null) {
            this.setType = SetType.NORMAL;
        }
        if (this.loadUnit == null) {
            this.loadUnit = LoadUnit.KG;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
