package it.aredegalli.coachly.workout.service;

import it.aredegalli.coachly.workout.controller.request.WorkoutSessionEventBatchRequest;
import it.aredegalli.coachly.workout.controller.response.WorkoutSessionEventBatchResponse;
import it.aredegalli.coachly.workout.model.WorkoutSession;
import it.aredegalli.coachly.workout.repository.WorkoutSessionEventRepository;
import it.aredegalli.coachly.workout.repository.WorkoutSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutSessionEventServiceTest {

    @Mock
    private WorkoutSessionEventRepository eventRepository;

    @Mock
    private WorkoutSessionRepository sessionRepository;

    @InjectMocks
    private WorkoutSessionEventService service;

    private static WorkoutSessionEventBatchRequest batchOf(int... sequences) {
        WorkoutSessionEventBatchRequest request = new WorkoutSessionEventBatchRequest();
        request.setEvents(java.util.Arrays.stream(sequences).mapToObj(seq -> {
            WorkoutSessionEventBatchRequest.Event event = new WorkoutSessionEventBatchRequest.Event();
            event.setId(UUID.randomUUID());
            event.setSeq(seq);
            event.setOccurredAt(OffsetDateTime.now());
            event.setType("set_completed");
            event.setPayload(Map.of("reps", 10));
            return event;
        }).toList());
        return request;
    }

    @Test
    void countsAcceptedAndDuplicateSeparately() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());
        // Il primo evento entra, il secondo era gia' presente.
        when(eventRepository.appendIfAbsent(
            any(), eq(sessionId), eq(userId), anyInt(), any(), anyString(), anyString(), any()
        )).thenReturn(1, 0);

        WorkoutSessionEventBatchResponse response =
            service.appendEvents(userId, sessionId, batchOf(1, 2));

        assertEquals(1, response.accepted());
        assertEquals(1, response.duplicate());
    }

    @Test
    void acceptsEventsForASessionThatHasNotArrivedYet() {
        // Le due code dell'outbox non salgono nello stesso ordine: rifiutare
        // qui significherebbe buttare via telemetria che non torna piu'.
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());
        when(eventRepository.appendIfAbsent(
            any(), any(), any(), anyInt(), any(), anyString(), anyString(), any()
        )).thenReturn(1);

        WorkoutSessionEventBatchResponse response =
            service.appendEvents(userId, sessionId, batchOf(1));

        assertEquals(1, response.accepted());
    }

    @Test
    void rejectsEventsForAnotherUsersSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        WorkoutSession foreign = WorkoutSession.builder()
            .id(sessionId)
            .userId(UUID.randomUUID())
            .build();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(foreign));

        assertThrows(
            ResponseStatusException.class,
            () -> service.appendEvents(userId, sessionId, batchOf(1))
        );
        verify(eventRepository, never()).appendIfAbsent(
            any(), any(), any(), anyInt(), any(), anyString(), anyString(), any()
        );
    }

    @Test
    void rejectsTwoEventsWithTheSameSeqInOneBatch() {
        // Senza questo controllo il secondo sparirebbe in silenzio per via
        // dell'ON CONFLICT, e nessuno se ne accorgerebbe.
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        assertThrows(
            ResponseStatusException.class,
            () -> service.appendEvents(userId, sessionId, batchOf(7, 7))
        );
        verify(eventRepository, never()).appendIfAbsent(
            any(), any(), any(), anyInt(), any(), anyString(), anyString(), any()
        );
    }
}
