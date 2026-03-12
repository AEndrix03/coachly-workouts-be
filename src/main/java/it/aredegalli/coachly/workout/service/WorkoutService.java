package it.aredegalli.coachly.workout.service;

import it.aredegalli.coachly.workout.dto.WorkoutDto;
import it.aredegalli.coachly.workout.mapper.WorkoutMapper;
import it.aredegalli.coachly.workout.repository.WorkoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final WorkoutMapper workoutMapper;

    public WorkoutService(WorkoutRepository workoutRepository, WorkoutMapper workoutMapper) {
        this.workoutRepository = workoutRepository;
        this.workoutMapper = workoutMapper;
    }

    @Transactional(readOnly = true)
    public List<WorkoutDto> getUserWorkouts(UUID userId) {
        return workoutMapper.toDtoList(workoutRepository.findAllByUserIdOrderByUpdatedAtDesc(userId));
    }
}
