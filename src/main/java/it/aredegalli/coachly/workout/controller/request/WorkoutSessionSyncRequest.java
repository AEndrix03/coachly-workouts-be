package it.aredegalli.coachly.workout.controller.request;

import it.aredegalli.coachly.workout.enums.LoadUnit;
import it.aredegalli.coachly.workout.enums.SetType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class WorkoutSessionSyncRequest {

    private UUID clientSessionId;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private String notes;
    private List<Entry> entries = new ArrayList<>();

    @Data
    public static class Entry {
        private UUID exerciseId;
        private Integer position;
        private Boolean completed;
        private String notes;
        private List<SetRow> sets = new ArrayList<>();
    }

    @Data
    public static class SetRow {
        private Integer position;
        private SetType setType;
        private Integer reps;
        private BigDecimal load;
        private LoadUnit loadUnit;
        private Boolean completed;
        private String notes;
    }
}
