package it.aredegalli.coachly.workout.service;

import it.aredegalli.coachly.workout.dto.WorkoutDto;
import it.aredegalli.coachly.workout.dto.command.WorkoutBlockEntryUpsertRequestDto;
import it.aredegalli.coachly.workout.dto.command.WorkoutBlockUpsertRequestDto;
import it.aredegalli.coachly.workout.dto.command.WorkoutSetUpsertRequestDto;
import it.aredegalli.coachly.workout.dto.command.WorkoutUpsertRequestDto;
import it.aredegalli.coachly.workout.enums.IntensityType;
import it.aredegalli.coachly.workout.enums.WorkoutGroupType;
import it.aredegalli.coachly.workout.mapper.WorkoutMapper;
import it.aredegalli.coachly.workout.model.Workout;
import it.aredegalli.coachly.workout.repository.WorkoutRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    @Mock
    private WorkoutRepository workoutRepository;

    @Mock
    private WorkoutMapper workoutMapper;

    @InjectMocks
    private WorkoutService workoutService;

    @Test
    void getUserWorkoutsReturnsMappedDtosForUser() {
        UUID userId = UUID.randomUUID();
        List<Workout> workouts = List.of(Workout.builder().id(UUID.randomUUID()).userId(userId).build());
        List<WorkoutDto> workoutDtos = List.of(WorkoutDto.builder().id(UUID.randomUUID()).userId(userId).build());

        when(workoutRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(workouts);
        when(workoutMapper.toDtoList(workouts)).thenReturn(workoutDtos);

        List<WorkoutDto> result = workoutService.getUserWorkouts(userId);

        assertSame(workoutDtos, result);
        verify(workoutRepository).findAllByUserIdOrderByUpdatedAtDesc(userId);
        verify(workoutMapper).toDtoList(workouts);
    }

    @Test
    void rejectsExerciseGroupWithOnlyOneEntry() {
        UUID userId = UUID.randomUUID();
        WorkoutUpsertRequestDto request = WorkoutUpsertRequestDto.builder()
            .name("Upper A")
            .blocks(List.of(WorkoutBlockUpsertRequestDto.builder()
                .groupType(WorkoutGroupType.SUPERSET)
                .entries(List.of(WorkoutBlockEntryUpsertRequestDto.builder()
                    .exerciseId(UUID.randomUUID())
                    .build()))
                .build()))
            .build();

        assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> workoutService.createWorkout(userId, request)
        );
    }

    @Test
    void rejectsInvertedRepAndIntensityRanges() {
        UUID userId = UUID.randomUUID();
        WorkoutSetUpsertRequestDto set = WorkoutSetUpsertRequestDto.builder()
            .repsMin((short) 12)
            .repsMax((short) 8)
            .intensityType(IntensityType.RIR)
            .intensityMin(BigDecimal.ONE)
            .intensityMax(BigDecimal.valueOf(2))
            .build();
        WorkoutUpsertRequestDto request = WorkoutUpsertRequestDto.builder()
            .name("Upper A")
            .blocks(List.of(WorkoutBlockUpsertRequestDto.builder()
                .groupType(WorkoutGroupType.EXERCISE)
                .entries(List.of(WorkoutBlockEntryUpsertRequestDto.builder()
                    .exerciseId(UUID.randomUUID())
                    .sets(List.of(set))
                    .build()))
                .build()))
            .build();

        assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> workoutService.createWorkout(userId, request)
        );
    }
}
