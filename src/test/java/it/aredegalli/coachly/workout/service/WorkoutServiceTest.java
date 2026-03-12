package it.aredegalli.coachly.workout.service;

import it.aredegalli.coachly.workout.dto.WorkoutDto;
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

import static org.junit.jupiter.api.Assertions.assertSame;
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
}
