package it.aredegalli.coachly.workout.controller;

import it.aredegalli.coachly.user.commons.services.AuditRetriever;
import it.aredegalli.coachly.workout.dto.WorkoutDto;
import it.aredegalli.coachly.workout.service.WorkoutService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/workouts")
public class WorkoutController {

    private final AuditRetriever auditRetriever;
    private final WorkoutService workoutService;

    public WorkoutController(AuditRetriever auditRetriever, WorkoutService workoutService) {
        this.auditRetriever = auditRetriever;
        this.workoutService = workoutService;
    }

    @GetMapping("/user")
    public List<WorkoutDto> getUserWorkouts() {
        UUID userId = auditRetriever.retrieve().getUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing or invalid X-User-Id header");
        }
        return workoutService.getUserWorkouts(userId);
    }
}
