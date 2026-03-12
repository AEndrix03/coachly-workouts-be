package it.aredegalli.coachly.workout.controller;

import it.aredegalli.coachly.user.commons.dto.AuditDto;
import it.aredegalli.coachly.user.commons.services.AuditRetriever;
import it.aredegalli.coachly.user.commons.utils.constants.AuditConstants;
import it.aredegalli.coachly.workout.dto.WorkoutDto;
import it.aredegalli.coachly.workout.service.WorkoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WorkoutControllerTest {

    @Mock
    private AuditRetriever auditRetriever;

    @Mock
    private WorkoutService workoutService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new WorkoutController(auditRetriever, workoutService)).build();
    }

    @Test
    void getUserWorkoutsReturnsDtosFromHeaderUserId() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID workoutId = UUID.randomUUID();

        when(auditRetriever.retrieve()).thenReturn(AuditDto.builder().userId(userId).build());
        when(workoutService.getUserWorkouts(userId)).thenReturn(List.of(
                WorkoutDto.builder()
                        .id(workoutId)
                        .userId(userId)
                        .name("Upper Body")
                        .translations("{}")
                        .build()
        ));

        mockMvc.perform(get("/workouts/user")
                        .header(AuditConstants.USER_ID_HEADER, userId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(workoutId.toString()))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].name").value("Upper Body"));

        verify(workoutService).getUserWorkouts(userId);
    }

    @Test
    void getUserWorkoutsRejectsMissingHeader() throws Exception {
        when(auditRetriever.retrieve()).thenReturn(AuditDto.builder().build());

        mockMvc.perform(get("/workouts/user").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
