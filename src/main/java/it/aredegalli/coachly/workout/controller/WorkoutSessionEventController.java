package it.aredegalli.coachly.workout.controller;

import it.aredegalli.coachly.user.commons.services.AuditRetriever;
import it.aredegalli.coachly.workout.controller.request.WorkoutSessionEventBatchRequest;
import it.aredegalli.coachly.workout.controller.response.WorkoutSessionEventBatchResponse;
import it.aredegalli.coachly.workout.service.WorkoutSessionEventService;
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

/**
 * Event log delle sessioni.
 *
 * <p>La rotta sta sotto {@code /workouts/sessions/} e non sotto
 * {@code /workouts/{workoutId}/sessions/} perche' una sessione puo' non avere
 * una scheda: gli allenamenti liberi hanno {@code workoutId} nullo, e legare
 * l'append degli eventi a una scheda li renderebbe impossibili da
 * sincronizzare.
 */
@RestController
@RequestMapping("/workouts/sessions")
public class WorkoutSessionEventController {

    private final AuditRetriever auditRetriever;
    private final WorkoutSessionEventService eventService;

    public WorkoutSessionEventController(
        AuditRetriever auditRetriever,
        WorkoutSessionEventService eventService
    ) {
        this.auditRetriever = auditRetriever;
        this.eventService = eventService;
    }

    @PostMapping("/{sessionId}/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public WorkoutSessionEventBatchResponse appendEvents(
        @PathVariable UUID sessionId,
        @Valid @RequestBody WorkoutSessionEventBatchRequest request
    ) {
        return eventService.appendEvents(requireUserId(), sessionId, request);
    }

    private UUID requireUserId() {
        UUID userId = auditRetriever.retrieve().getUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing or invalid X-User-Id header");
        }
        return userId;
    }
}
