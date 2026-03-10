package it.aredegalli.coachly.workout.mapper;

import it.aredegalli.coachly.workout.dto.WorkoutSessionDto;
import it.aredegalli.coachly.workout.dto.snapshot.SessionSnapshotDto;
import it.aredegalli.coachly.workout.dto.snapshot.SnapshotBlockDto;
import it.aredegalli.coachly.workout.enums.SessionStatus;
import it.aredegalli.coachly.workout.model.WorkoutSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = WorkoutSessionMapperImpl.class)
class WorkoutSessionMapperTest {

    @Autowired
    private WorkoutSessionMapper workoutSessionMapper;

    @Test
    void mapsSessionSnapshotToAndFromJson() {
        SessionSnapshotDto snapshot = new SessionSnapshotDto();
        snapshot.setWorkoutName("Push Day");
        SnapshotBlockDto block = new SnapshotBlockDto();
        block.setLabel("A");
        block.setPosition(0);
        snapshot.setBlocks(List.of(block));

        WorkoutSessionDto dto = WorkoutSessionDto.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .status(SessionStatus.COMPLETED)
                .startedAt(OffsetDateTime.now())
                .snapshot(snapshot)
                .build();

        WorkoutSession entity = workoutSessionMapper.toEntity(dto);

        assertNotNull(entity.getSnapshot());
        assertEquals(SessionStatus.COMPLETED, entity.getStatus());

        WorkoutSession roundTripSource = WorkoutSession.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .status(entity.getStatus())
                .startedAt(dto.getStartedAt())
                .snapshot(entity.getSnapshot())
                .build();

        WorkoutSessionDto roundTrip = workoutSessionMapper.toDto(roundTripSource);

        assertNotNull(roundTrip.getSnapshot());
        assertEquals("Push Day", roundTrip.getSnapshot().getWorkoutName());
        assertEquals("A", roundTrip.getSnapshot().getBlocks().getFirst().getLabel());
    }
}
