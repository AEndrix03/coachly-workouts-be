package it.aredegalli.coachly.workout.mapper;

import it.aredegalli.coachly.workout.dto.WorkoutSetDto;
import it.aredegalli.coachly.workout.model.WorkoutSet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WorkoutSetMapper {

    @Mapping(target = "entryId", source = "entry.id")
    WorkoutSetDto toDto(WorkoutSet entity);

    List<WorkoutSetDto> toDtoList(List<WorkoutSet> entities);

    @Mapping(target = "entry", ignore = true)
    WorkoutSet toEntity(WorkoutSetDto dto);

    List<WorkoutSet> toEntityList(List<WorkoutSetDto> dtos);
}
