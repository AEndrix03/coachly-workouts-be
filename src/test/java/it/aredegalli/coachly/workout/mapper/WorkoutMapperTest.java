package it.aredegalli.coachly.workout.mapper;

import it.aredegalli.coachly.workout.dto.WorkoutBlockDto;
import it.aredegalli.coachly.workout.dto.WorkoutBlockEntryDto;
import it.aredegalli.coachly.workout.dto.WorkoutDto;
import it.aredegalli.coachly.workout.dto.WorkoutSetDto;
import it.aredegalli.coachly.workout.enums.LoadUnit;
import it.aredegalli.coachly.workout.enums.SetType;
import it.aredegalli.coachly.workout.enums.WorkoutStatus;
import it.aredegalli.coachly.workout.model.Workout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        WorkoutMapperImpl.class,
        WorkoutBlockMapperImpl.class,
        WorkoutBlockEntryMapperImpl.class,
        WorkoutSetMapperImpl.class
})
class WorkoutMapperTest {

    @Autowired
    private WorkoutMapper workoutMapper;

    @Test
    void toEntityLinksNestedWorkoutGraph() {
        WorkoutDto dto = WorkoutDto.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .name("Upper Body")
                .translations("{\"it\":{\"title\":\"Upper Body\"}}")
                .status(WorkoutStatus.ACTIVE)
                .blocks(List.of(
                        WorkoutBlockDto.builder()
                                .id(UUID.randomUUID())
                                .position((short) 0)
                                .label("A")
                                .entries(List.of(
                                        WorkoutBlockEntryDto.builder()
                                                .id(UUID.randomUUID())
                                                .exerciseId(UUID.randomUUID())
                                                .position((short) 0)
                                                .sets(List.of(
                                                        WorkoutSetDto.builder()
                                                                .id(UUID.randomUUID())
                                                                .position((short) 0)
                                                                .setType(SetType.NORMAL)
                                                                .loadUnit(LoadUnit.KG)
                                                                .build()
                                                ))
                                                .build()
                                ))
                                .build()
                ))
                .build();

        Workout entity = workoutMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals(dto.getId(), entity.getId());
        assertEquals(1, entity.getBlocks().size());
        assertSame(entity, entity.getBlocks().getFirst().getWorkout());
        assertSame(entity.getBlocks().getFirst(), entity.getBlocks().getFirst().getEntries().getFirst().getBlock());
        assertSame(
                entity.getBlocks().getFirst().getEntries().getFirst(),
                entity.getBlocks().getFirst().getEntries().getFirst().getSets().getFirst().getEntry()
        );
    }
}
