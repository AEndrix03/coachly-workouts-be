package it.aredegalli.coachly.workout.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.aredegalli.coachly.workout.controller.request.WorkoutSessionEventBatchRequest;
import it.aredegalli.coachly.workout.controller.response.WorkoutSessionEventBatchResponse;
import it.aredegalli.coachly.workout.repository.WorkoutSessionEventRepository;
import it.aredegalli.coachly.workout.repository.WorkoutSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class WorkoutSessionEventService {

    private final WorkoutSessionEventRepository eventRepository;
    private final WorkoutSessionRepository sessionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorkoutSessionEventService(
        WorkoutSessionEventRepository eventRepository,
        WorkoutSessionRepository sessionRepository
    ) {
        this.eventRepository = eventRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Appende un batch di eventi a una sessione.
     *
     * <p>Due scelte che vale la pena dichiarare.
     *
     * <p><strong>Una sessione sconosciuta non e' un errore.</strong> Gli eventi
     * possono arrivare prima della sessione a cui appartengono: l'outbox del
     * client non garantisce che le due code salgano nello stesso ordine, e un
     * 404 qui verrebbe classificato dal client come fallimento permanente,
     * buttando via telemetria che non torna piu'. Se invece la sessione esiste
     * ed e' di un altro utente, allora si rifiuta.
     *
     * <p><strong>Il batch non e' atomico.</strong> Un duplicato in mezzo non
     * annulla i vicini: l'append e' idempotente per riga, non per richiesta.
     */
    @Transactional
    public WorkoutSessionEventBatchResponse appendEvents(
        UUID userId,
        UUID sessionId,
        WorkoutSessionEventBatchRequest request
    ) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            if (!session.getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
            }
        });

        rejectDuplicateSequencesWithinBatch(request);

        OffsetDateTime receivedAt = OffsetDateTime.now();
        int accepted = 0;

        for (WorkoutSessionEventBatchRequest.Event event : request.getEvents()) {
            int inserted = eventRepository.appendIfAbsent(
                event.getId(),
                sessionId,
                userId,
                event.getSeq(),
                event.getOccurredAt(),
                event.getType(),
                serializePayload(event.getPayload()),
                receivedAt
            );
            accepted += inserted;
        }

        return new WorkoutSessionEventBatchResponse(
            accepted,
            request.getEvents().size() - accepted
        );
    }

    /**
     * Due eventi con lo stesso {@code seq} nella stessa richiesta sono un bug
     * del client, non un reinvio: il secondo sparirebbe in silenzio per via
     * dell'{@code ON CONFLICT}, e nessuno se ne accorgerebbe mai.
     */
    private void rejectDuplicateSequencesWithinBatch(WorkoutSessionEventBatchRequest request) {
        Set<Integer> seen = new HashSet<>();
        for (WorkoutSessionEventBatchRequest.Event event : request.getEvents()) {
            if (!seen.add(event.getSeq())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duplicate seq " + event.getSeq() + " within the same batch"
                );
            }
        }
    }

    private String serializePayload(Object payload) {
        if (payload == null) return "{}";
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Event payload is not serializable", e
            );
        }
    }
}
