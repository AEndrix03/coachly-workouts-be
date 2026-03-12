package it.aredegalli.coachly.workout.controller;

import it.aredegalli.coachly.workout.dto.WorkoutDto;
import it.aredegalli.coachly.workout.service.WorkoutService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/workouts")
public class WorkoutController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @GetMapping("/user")
    public List<WorkoutDto> getUserWorkouts(@RequestHeader(USER_ID_HEADER) UUID userId) {
        return workoutService.getUserWorkouts(userId);
    }
}
