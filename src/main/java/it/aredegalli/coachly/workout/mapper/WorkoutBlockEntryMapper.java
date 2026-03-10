package it.aredegalli.coachly.workout.mapper;

import it.aredegalli.coachly.workout.dto.WorkoutBlockEntryDto;
import it.aredegalli.coachly.workout.model.WorkoutBlock;
import it.aredegalli.coachly.workout.model.WorkoutBlockEntry;
import it.aredegalli.coachly.workout.model.WorkoutSet;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = WorkoutSetMapper.class)
public interface WorkoutBlockEntryMapper {

    @Mapping(target = "blockId", source = "block.id")
    WorkoutBlockEntryDto toDto(WorkoutBlockEntry entity);

    List<WorkoutBlockEntryDto> toDtoList(List<WorkoutBlockEntry> entities);

    @Mapping(target = "block", ignore = true)
    WorkoutBlockEntry toEntity(WorkoutBlockEntryDto dto);

    List<WorkoutBlockEntry> toEntityList(List<WorkoutBlockEntryDto> dtos);

    @AfterMapping
    default void linkChildren(@MappingTarget WorkoutBlockEntry entry) {
        if (entry.getSets() == null) {
            return;
        }
        for (WorkoutSet set : entry.getSets()) {
            set.setEntry(entry);
        }
    }

    default void attachToBlock(WorkoutBlockEntry entry, WorkoutBlock block) {
        entry.setBlock(block);
        linkChildren(entry);
    }
}
