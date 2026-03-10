package it.aredegalli.coachly.workout.mapper;

import it.aredegalli.coachly.workout.dto.WorkoutDto;
import it.aredegalli.coachly.workout.model.Workout;
import it.aredegalli.coachly.workout.model.WorkoutBlock;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = WorkoutBlockMapper.class)
public interface WorkoutMapper {

    WorkoutDto toDto(Workout entity);

    List<WorkoutDto> toDtoList(List<Workout> entities);

    Workout toEntity(WorkoutDto dto);

    List<Workout> toEntityList(List<WorkoutDto> dtos);

    @AfterMapping
    default void linkChildren(@MappingTarget Workout workout) {
        if (workout.getBlocks() == null) {
            return;
        }
        for (WorkoutBlock block : workout.getBlocks()) {
            block.setWorkout(workout);
            if (block.getEntries() == null) {
                continue;
            }
            block.getEntries().forEach(entry -> {
                entry.setBlock(block);
                if (entry.getSets() != null) {
                    entry.getSets().forEach(set -> set.setEntry(entry));
                }
            });
        }
    }
}
