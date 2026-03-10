package it.aredegalli.coachly.workout.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An ordered group of exercises within a workout.
 *
 * <p>A block with a single {@link WorkoutBlockEntry} represents a normal
 * exercise. A block with two or more entries is a <strong>superset</strong>
 * — the entries are executed in sequence (A→B→A→B→...) before the block
 * rest is applied.
 *
 * <p>{@link #restSeconds} defines the rest period <em>after completing all
 * sets of the block</em>. Individual sets can override this via
 * {@link WorkoutSet#restSeconds}.
 *
 * <p>Client-generated UUID — see {@link Workout} for offline-first rationale.
 */
@Entity
@Table(
        schema = "workout",
        name = "workout_block",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_workout_block_position",
                columnNames = {"workout_id", "position"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutBlock {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Parent workout. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_id", nullable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Workout workout;

    /**
     * 0-based position within the workout. Managed entirely by the client —
     * the server stores whatever position is sent on sync.
     */
    @Column(name = "position", nullable = false)
    private Short position;

    /**
     * Optional UI label, e.g. "A", "B", "Superset 1".
     * Displayed as a header in the exercise list.
     */
    @Column(name = "label", length = 50)
    private String label;

    /**
     * Rest time in seconds after completing all sets of this block.
     * May be overridden at the individual set level via {@link WorkoutSet#restSeconds}.
     * Null means no specific rest is prescribed.
     */
    @Column(name = "rest_seconds")
    private Short restSeconds;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ── Relations ────────────────────────────────────────────

    /**
     * Exercise slots in this block, ordered by position.
     * Single entry = normal set. Multiple entries = superset.
     */
    @OneToMany(mappedBy = "block", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    @Builder.Default
    private List<WorkoutBlockEntry> entries = new ArrayList<>();

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