package it.aredegalli.coachly.workout.controller;

import it.aredegalli.coachly.user.commons.services.AuditRetriever;
import it.aredegalli.coachly.workout.controller.request.WorkoutSessionSyncRequest;
import it.aredegalli.coachly.workout.service.WorkoutSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/workouts")
public class WorkoutSessionController {

    private final AuditRetriever auditRetriever;
    private final WorkoutSessionService workoutSessionService;

    public WorkoutSessionController(AuditRetriever auditRetriever, WorkoutSessionService workoutSessionService) {
        this.auditRetriever = auditRetriever;
        this.workoutSessionService = workoutSessionService;
    }

    @PostMapping("/{workoutId}/sessions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void syncWorkoutSession(
        @PathVariable UUID workoutId,
        @Valid @RequestBody WorkoutSessionSyncRequest request
    ) {
        workoutSessionService.syncWorkoutSession(requireUserId(), workoutId, request);
    }

    private UUID requireUserId() {
        UUID userId = auditRetriever.retrieve().getUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing or invalid X-User-Id header");
        }
        return userId;
    }
}
