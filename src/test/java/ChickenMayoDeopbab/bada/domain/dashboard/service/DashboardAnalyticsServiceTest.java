package ChickenMayoDeopbab.bada.domain.dashboard.service;

import ChickenMayoDeopbab.bada.domain.dashboard.dto.request.DashboardQuery;
import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.projection.TrainingDashboardRow;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DashboardAnalyticsServiceTest {
    private final TrainingRecordRepository records = mock(TrainingRecordRepository.class);
    private final UsersRepository users = mock(UsersRepository.class);
    private final Users user = mock(Users.class);
    private final DashboardAnalyticsService service = new DashboardAnalyticsService(records, users, new DashboardAggregator(),
            Clock.fixed(Instant.parse("2026-10-17T00:00:00Z"), ZoneId.of("Asia/Seoul")));

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("owner", null));
        when(users.findByUsername("owner")).thenReturn(Optional.of(user));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void distinguishes_new_scenarios_from_repeated_practice_using_retained_history() {
        when(records.findDashboardRows(eq(user), any(), any(), any())).thenReturn(List.of(
                row(1, 12, 12, 1), row(2, 12, 13, 3), row(3, 13, 14, 3)));
        when(records.findFirstScenarioTrainings(eq(user), any(), any())).thenReturn(List.of(
                new TrainingDashboardRow.FirstTraining(12L, SessionType.SCENARIO, LocalDateTime.of(2026, 8, 1, 0, 0)),
                new TrainingDashboardRow.FirstTraining(13L, SessionType.SCENARIO, LocalDateTime.of(2026, 10, 14, 0, 0))));
        var result = service.overview(query(null, null, null, null));
        assertThat(result.summary().trainingCount()).isEqualTo(3);
        assertThat(result.summary().trainingDays()).isEqualTo(3);
        assertThat(result.experience().scenarioCount()).isEqualTo(2);
        assertThat(result.experience().newScenarioCount()).isEqualTo(1);
        assertThat(result.hintSettings()).containsEntry("1", 1L).containsEntry("3", 2L);
    }

    @Test
    void paginates_scenarios_with_stable_ties_and_keeps_conditions_separate() {
        when(records.findDashboardRows(eq(user), eq(LocalDateTime.of(2026, 10, 10, 0, 0)), any(), any()))
                .thenReturn(List.of(row(1, 12, 15, 1), row(2, 12, 15, 3), row(3, 13, 15, 1), row(4, 14, 14, 1)));
        when(records.findDashboardRows(eq(user), eq(LocalDateTime.of(2026, 10, 3, 0, 0)), any(), any())).thenReturn(List.of());
        when(records.findFirstScenarioTrainings(eq(user), any(), any())).thenReturn(List.of());
        var first = service.scenarios(query(0, 1, null, null));
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(first.items().getFirst().scenarioId()).isEqualTo(12);
        assertThat(first.items().getFirst().conditionBreakdown()).hasSize(2);
        var second = service.scenarios(query(1, 1, null, null));
        assertThat(second.items().getFirst().scenarioId()).isEqualTo(13);
        assertThat(service.scenarios(query(Integer.MAX_VALUE, 50, null, null)).items()).isEmpty();
    }

    @Test
    void only_includes_scenario_series_when_requested() {
        when(records.findDashboardRows(eq(user), eq(LocalDateTime.of(2026, 10, 10, 0, 0)), any(), any()))
                .thenReturn(List.of(row(1, 12, 15, 1)));
        when(records.findDashboardRows(eq(user), eq(LocalDateTime.of(2026, 10, 3, 0, 0)), any(), any())).thenReturn(List.of());
        when(records.findFirstScenarioTrainings(eq(user), any(), any())).thenReturn(List.of());
        assertThat(service.scenarios(query(0, 20, 12L, true)).items().getFirst().series()).hasSize(7);
        assertThat(service.scenarios(query(0, 20, 12L, false)).items().getFirst().series()).isEmpty();
    }

    private DashboardQuery query(Integer page, Integer size, Long scenarioId, Boolean series) {
        return new DashboardQuery(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 16), DashboardQuery.Interval.DAY,
                null, scenarioId, null, null, null, null, null, null, page, size, series);
    }

    private TrainingDashboardRow row(long id, long scenario, int day, int hints) {
        return new TrainingDashboardRow(id, scenario, "예약", "SCENARIO_V1", "EASY", hints, SessionType.SCENARIO,
                AiPersonality.KIND, EndReason.END_CALL, LocalDateTime.of(2026, 10, day, 12, 0), 60L, (short) 3, null);
    }
}
