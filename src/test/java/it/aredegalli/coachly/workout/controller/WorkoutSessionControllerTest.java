package it.aredegalli.coachly.workout.controller;

import it.aredegalli.coachly.user.commons.dto.AuditDto;
import it.aredegalli.coachly.user.commons.services.AuditRetriever;
import it.aredegalli.coachly.workout.service.WorkoutSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WorkoutSessionControllerTest {

    @Mock
    private AuditRetriever auditRetriever;

    @Mock
    private WorkoutSessionService workoutSessionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
            new WorkoutSessionController(auditRetriever, workoutSessionService)
        ).build();
    }

    @Test
    void syncWorkoutSessionAcceptsPayloadAndDelegatesToService() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID workoutId = UUID.randomUUID();

        when(auditRetriever.retrieve()).thenReturn(AuditDto.builder().userId(userId).build());

        mockMvc.perform(post("/workouts/{workoutId}/sessions", workoutId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entries\":[]}"))
            .andExpect(status().isAccepted());

        verify(workoutSessionService).syncWorkoutSession(eq(userId), eq(workoutId), any());
    }
}
