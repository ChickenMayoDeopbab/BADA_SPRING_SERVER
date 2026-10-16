package ChickenMayoDeopbab.bada.domain.trainingrecord.controller;

import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request.TrainingRecordSearchQuery;
import ChickenMayoDeopbab.bada.domain.trainingrecord.service.RecordingPlaybackService;
import ChickenMayoDeopbab.bada.domain.trainingrecord.service.TrainingRecordService;
import ChickenMayoDeopbab.bada.global.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrainingRecordSearchControllerTest {
    private final TrainingRecordService service = mock(TrainingRecordService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new TrainingRecordController(service, mock(RecordingPlaybackService.class)))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void binds_date_and_scenario_filters() throws Exception {
        when(service.getTrainingRecords(any(Pageable.class), any())).thenAnswer(call -> Page.empty(call.getArgument(0)));
        mvc.perform(get("/api/v1/training-records").param("from", "2026-10-01").param("to", "2026-10-02")
                .param("scenarioId", "12").param("scriptLevel", "3").accept("application/json"))
                .andExpect(status().isOk());
        ArgumentCaptor<TrainingRecordSearchQuery> query = ArgumentCaptor.forClass(TrainingRecordSearchQuery.class);
        verify(service).getTrainingRecords(any(), query.capture());
        assertThat(query.getValue().from()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(query.getValue().scenarioId()).isEqualTo(12);
        assertThat(query.getValue().scriptLevel()).isEqualTo(3);
    }

    @Test
    void invalid_date_binding_returns_bad_request() throws Exception {
        mvc.perform(get("/api/v1/training-records").param("from", "invalid").accept("application/json"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
