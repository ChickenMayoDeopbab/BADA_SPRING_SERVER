package ChickenMayoDeopbab.bada.domain.dashboard.controller;

import ChickenMayoDeopbab.bada.domain.dashboard.service.DashboardAggregator;
import ChickenMayoDeopbab.bada.domain.dashboard.service.DashboardAnalyticsService;
import ChickenMayoDeopbab.bada.domain.dashboard.service.DashboardService;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.handler.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DashboardAnalyticsControllerTest {
    private MockMvc mvc;
    private TrainingRecordRepository records;
    private Users user;

    @BeforeEach
    void setup() {
        records = mock(TrainingRecordRepository.class);
        UsersRepository users = mock(UsersRepository.class);
        user = mock(Users.class);
        when(users.findByUsername("tester")).thenReturn(Optional.of(user));
        when(records.findDashboardRows(eq(user), any(), any(), any())).thenReturn(List.of());
        var service = new DashboardAnalyticsService(records, users, new DashboardAggregator(),
                Clock.fixed(Instant.parse("2026-10-17T03:00:00Z"), ZoneId.of("Asia/Seoul")));
        mvc = MockMvcBuilders.standaloneSetup(new DashboardController(mock(DashboardService.class), service))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json()
                        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build()))
                .setControllerAdvice(new DashboardRequestExceptionHandler(), new GlobalExceptionHandler()).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("tester", null, List.of()));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void defaults_to_korean_recent_period_and_preserves_missing_values() throws Exception {
        mvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.meta.period.from").value("2026-09-20"))
                .andExpect(jsonPath("$.data.meta.period.to").value("2026-10-17"))
                .andExpect(jsonPath("$.data.meta.timeZone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.data.summary.trainingCount").value(0))
                .andExpect(jsonPath("$.data.metrics.selfReportedAnxiety.value").value(nullValue()))
                .andExpect(jsonPath("$.data.activity.length()").value(28));
        verify(records).findDashboardRows(eq(user), eq(LocalDateTime.of(2026, 9, 20, 0, 0)),
                eq(LocalDateTime.of(2026, 10, 18, 0, 0)), any());
    }

    @Test
    void provides_comparison_and_scenario_endpoints_without_data() throws Exception {
        mvc.perform(get("/api/v1/dashboard/comparison"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meta.baseline.from").value("2026-08-23"))
                .andExpect(jsonPath("$.data.meta.baseline.to").value("2026-09-19"))
                .andExpect(jsonPath("$.data.metrics[0].matched.delta").value(nullValue()));
        mvc.perform(get("/api/v1/dashboard/scenarios").param("scenarioId", "12").param("includeSeries", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @ParameterizedTest
    @CsvSource({"from,not-a-date", "interval,MONTH", "scriptLevel,0", "sessionType,WARMUP", "scenarioId,-1",
            "page,-1", "size,51", "includeSeries,true"})
    void invalid_query_is_a_client_error(String key, String value) throws Exception {
        mvc.perform(get("/api/v1/dashboard/scenarios").param(key, value)).andExpect(status().isBadRequest());
        verifyNoInteractions(records);
    }

    @Test
    void rejects_future_overlapping_unpaired_and_oversized_periods() throws Exception {
        mvc.perform(get("/api/v1/dashboard/overview").param("from", "2026-10-17")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/dashboard/overview").param("from", "2026-10-17").param("to", "2026-10-18"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/dashboard/overview").param("from", "2025-01-01").param("to", "2026-10-17"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/dashboard/comparison").param("baselineFrom", "2026-09-20").param("baselineTo", "2026-10-17"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(records);
    }
}
