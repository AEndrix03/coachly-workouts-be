package it.aredegalli.coachly.workout.mapper;

import it.aredegalli.coachly.workout.dto.WorkoutBlockDto;
import it.aredegalli.coachly.workout.model.Workout;
import it.aredegalli.coachly.workout.model.WorkoutBlock;
import it.aredegalli.coachly.workout.model.WorkoutBlockEntry;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = WorkoutBlockEntryMapper.class)
public interface WorkoutBlockMapper {

    @Mapping(target = "workoutId", source = "workout.id")
    WorkoutBlockDto toDto(WorkoutBlock entity);

    List<WorkoutBlockDto> toDtoList(List<WorkoutBlock> entities);

    @Mapping(target = "workout", ignore = true)
    WorkoutBlock toEntity(WorkoutBlockDto dto);

    List<WorkoutBlock> toEntityList(List<WorkoutBlockDto> dtos);

    @AfterMapping
    default void linkChildren(@MappingTarget WorkoutBlock block) {
        if (block.getEntries() == null) {
            return;
        }
        for (WorkoutBlockEntry entry : block.getEntries()) {
            entry.setBlock(block);
        }
    }

    default void attachToWorkout(WorkoutBlock block, Workout workout) {
        block.setWorkout(workout);
        linkChildren(block);
    }
}
