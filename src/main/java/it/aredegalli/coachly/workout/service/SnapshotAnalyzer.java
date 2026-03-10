package it.aredegalli.coachly.workout.service;

import it.aredegalli.coachly.workout.dto.snapshot.SessionSnapshotDto;
import it.aredegalli.coachly.workout.dto.snapshot.SnapshotBlockDto;
import it.aredegalli.coachly.workout.dto.snapshot.SnapshotEntryDto;
import it.aredegalli.coachly.workout.dto.snapshot.SnapshotSetDto;
import it.aredegalli.coachly.workout.enums.LoadUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Extracts denormalized stats from a {@link SessionSnapshotDto}.
 *
 * <p>Called at every sync to populate {@code total_sets},
 * {@code completed_sets} and {@code total_volume_kg} on the
 * {@link com.coachly.workout.model.WorkoutSession} entity.
 *
 * <p>This avoids JSONB scans on every stats or dashboard query —
 * the extracted columns are indexed and queried directly.
 */
@Component
@RequiredArgsConstructor
public class SnapshotAnalyzer {

    private static final BigDecimal LBS_TO_KG = new BigDecimal("0.453592");

    public record SessionStats(
            short totalSets,
            short completedSets,
            BigDecimal totalVolumeKg
    ) {}

    /**
     * Iterates all sets in the snapshot and computes:
     * <ul>
     *   <li>{@code totalSets}   — non-skipped sets</li>
     *   <li>{@code completedSets} — sets with {@code completed = true}</li>
     *   <li>{@code totalVolumeKg} — sum of {@code actual_reps × actual_load_in_kg}
     *       for completed load-based sets</li>
     * </ul>
     */
    public SessionStats extract(SessionSnapshotDto snapshot) {
        if (snapshot == null || snapshot.getBlocks() == null) {
            return new SessionStats((short) 0, (short) 0, BigDecimal.ZERO);
        }

        int totalSets = 0;
        int completedSets = 0;
        BigDecimal totalVolumeKg = BigDecimal.ZERO;

        for (SnapshotBlockDto block : snapshot.getBlocks()) {
            if (block.getEntries() == null) continue;

            for (SnapshotEntryDto entry : block.getEntries()) {
                if (entry.getSets() == null) continue;

                for (SnapshotSetDto set : entry.getSets()) {
                    if (set.isSkipped()) continue;

                    totalSets++;

                    if (set.isCompleted()) {
                        completedSets++;
                        totalVolumeKg = totalVolumeKg.add(
                                computeVolumeKg(set)
                        );
                    }
                }
            }
        }

        return new SessionStats(
                (short) totalSets,
                (short) completedSets,
                totalVolumeKg.compareTo(BigDecimal.ZERO) == 0 ? null : totalVolumeKg
        );
    }

    /**
     * Computes the volume contribution of a single completed set in kg.
     *
     * <p>Returns {@link BigDecimal#ZERO} for sets where volume is not
     * meaningful: {@code bodyweight}, {@code band}, {@code machine_notch},
     * or when {@code actual_reps} / {@code actual_load} are null.
     */
    private BigDecimal computeVolumeKg(SnapshotSetDto set) {
        if (set.getActualReps() == null || set.getActualLoad() == null) {
            return BigDecimal.ZERO;
        }

        LoadUnit unit = set.getLoadUnit();
        if (unit == null
                || unit == LoadUnit.BODYWEIGHT
                || unit == LoadUnit.BAND
                || unit == LoadUnit.MACHINE_NOTCH) {
            return BigDecimal.ZERO;
        }

        BigDecimal loadKg = switch (unit) {
            case kg  -> set.getActualLoad();
            case lbs -> set.getActualLoad().multiply(LBS_TO_KG).setScale(2, RoundingMode.HALF_UP);
            default  -> BigDecimal.ZERO;
        };

        return loadKg.multiply(BigDecimal.valueOf(set.getActualReps()));
    }
}