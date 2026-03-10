package it.aredegalli.coachly.workout.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A single exercise slot inside a {@link WorkoutBlock}.
 *
 * <p>When the parent block has only one entry, the block represents a
 * regular exercise. When two or more entries are present, the block is
 * a <strong>superset</strong> and {@link #position} defines the execution
 * order within the superset rotation.
 *
 * <p>{@link #exerciseId} is an opaque external reference to the
 * {@code exercises.exercise} table in {@code catalog-service}.
 * There is intentionally no database-level FK — the reference is
 * resolved at the application layer via REST call to catalog-service.
 *
 * <p>Client-generated UUID — see {@link Workout} for offline-first rationale.
 */
@Entity
@Table(
        schema = "workout",
        name = "workout_block_entry",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_block_entry_position",
                columnNames = {"block_id", "position"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutBlockEntry {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Parent block. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "block_id", nullable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private WorkoutBlock block;

    /**
     * Opaque external reference to {@code exercises.exercise.id} in catalog-service.
     * Resolved at the application layer — no DB-level FK by design.
     */
    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    /**
     * 0-based order within the block.
     * Defines the superset rotation sequence (0=first exercise, 1=second, ...).
     */
    @Column(name = "position", nullable = false)
    private Short position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ── Relations ────────────────────────────────────────────

    /**
     * Planned sets for this exercise entry, ordered by position.
     */
    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    @Builder.Default
    private List<WorkoutSet> sets = new ArrayList<>();

    // ── Lifecycle hooks ──────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}