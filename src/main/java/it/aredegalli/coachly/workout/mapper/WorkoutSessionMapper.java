package it.aredegalli.coachly.workout.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.aredegalli.coachly.workout.dto.WorkoutSessionDto;
import it.aredegalli.coachly.workout.dto.snapshot.SessionSnapshotDto;
import it.aredegalli.coachly.workout.model.WorkoutSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WorkoutSessionMapper {

    ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    @Mapping(target = "snapshot", expression = "java(deserializeSnapshot(entity.getSnapshot()))")
    WorkoutSessionDto toDto(WorkoutSession entity);

    List<WorkoutSessionDto> toDtoList(List<WorkoutSession> entities);

    @Mapping(target = "snapshot", expression = "java(serializeSnapshot(dto.getSnapshot()))")
    WorkoutSession toEntity(WorkoutSessionDto dto);

    List<WorkoutSession> toEntityList(List<WorkoutSessionDto> dtos);

    default String serializeSnapshot(SessionSnapshotDto snapshot) {
        if (snapshot == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize workout session snapshot", exception);
        }
    }

    default SessionSnapshotDto deserializeSnapshot(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(snapshot, SessionSnapshotDto.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to deserialize workout session snapshot", exception);
        }
    }
}
