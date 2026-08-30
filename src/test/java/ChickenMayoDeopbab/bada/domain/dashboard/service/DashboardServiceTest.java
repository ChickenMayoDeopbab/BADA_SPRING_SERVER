package ChickenMayoDeopbab.bada.domain.dashboard.service;

import ChickenMayoDeopbab.bada.domain.callanxiety.entity.CallAnxietyState;
import ChickenMayoDeopbab.bada.domain.callanxiety.repository.CallAnxietyStateRepository;
import ChickenMayoDeopbab.bada.domain.dashboard.dto.response.DashboardMetricsResponse;
import ChickenMayoDeopbab.bada.domain.dashboard.dto.response.WeeklySummaryResponse;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request.TrainingAnalysisRequest;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.AnalysisQualityStatus;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingAnalysisMetrics;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(
                    Instant.parse("2026-08-26T03:00:00Z"),
                    ZoneId.of("Asia/Seoul")
            );

    @Mock
    private TrainingRecordRepository trainingRecordRepository;

    @Mock
    private CallAnxietyStateRepository callAnxietyStateRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private Users user;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                trainingRecordRepository,
                callAnxietyStateRepository,
                usersRepository,
                FIXED_CLOCK
        );

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "tester",
                                null,
                                List.of()
                        )
                );

        when(usersRepository.findByUsername("tester"))
                .thenReturn(Optional.of(user));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsSevenDailyMetricSlots() {
        List<TrainingRecord> records = List.of(
                record(
                        LocalDateTime.of(
                                2026, 8, 24, 10, 0
                        ),
                        60L,
                        EndReason.SCENARIO_DONE,
                        analysis("80", "70", "90", true)
                ),
                record(
                        LocalDateTime.of(
                                2026, 8, 24, 15, 0
                        ),
                        60L,
                        EndReason.SCENARIO_DONE,
                        analysis("100", "90", "70", true)
                ),
                record(
                        LocalDateTime.of(
                                2026, 8, 26, 12, 0
                        ),
                        60L,
                        EndReason.END_CALL,
                        analysis("60", "70", "80", true)
                ),
                record(
                        LocalDateTime.of(
                                2026, 8, 25, 12, 0
                        ),
                        60L,
                        EndReason.END_CALL,
                        analysis("10", "10", "10", false)
                )
        );

        when(trainingRecordRepository
                .findAllByUserAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtAsc(
                        user,
                        LocalDateTime.of(
                                2026, 8, 24, 0, 0
                        ),
                        LocalDateTime.of(
                                2026, 8, 31, 0, 0
                        )
                ))
                .thenReturn(records);

        CallAnxietyState state =
                CallAnxietyState.create(
                        user,
                        new BigDecimal("3.0"),
                        CallPhobiaLevel.LEVEL_3,
                        LocalDateTime.now(FIXED_CLOCK)
                );

        when(callAnxietyStateRepository.findByUser(user))
                .thenReturn(Optional.of(state));

        DashboardMetricsResponse response =
                dashboardService.getWeeklyMetrics();

        assertThat(response.dates()).hasSize(7);

        assertThat(response.stabilityScores())
                .containsExactly(
                        new BigDecimal("90.00"),
                        null,
                        new BigDecimal("60.00"),
                        null,
                        null,
                        null,
                        null
                );

        assertThat(response.conversationScores())
                .containsExactly(
                        new BigDecimal("80.00"),
                        null,
                        new BigDecimal("70.00"),
                        null,
                        null,
                        null,
                        null
                );

        assertThat(response.fluencyScores())
                .containsExactly(
                        new BigDecimal("80.00"),
                        null,
                        new BigDecimal("80.00"),
                        null,
                        null,
                        null,
                        null
                );

        assertThat(response.callPhobiaLevel().code())
                .isEqualTo("LEVEL_3");

        assertThat(response.callPhobiaLevel().name())
                .isEqualTo("통화 긴장형");
    }

    @Test
    void excludesErrorAndNoAudioFromSummary() {
        List<TrainingRecord> currentRecords = List.of(
                record(
                        LocalDateTime.of(
                                2026, 8, 24, 10, 0
                        ),
                        60L,
                        EndReason.SCENARIO_DONE,
                        analysis("90", "76", "60", true)
                ),
                record(
                        LocalDateTime.of(
                                2026, 8, 26, 10, 0
                        ),
                        120L,
                        EndReason.END_CALL,
                        analysis("90", "76", "60", true)
                ),
                record(
                        LocalDateTime.of(
                                2026, 8, 27, 10, 0
                        ),
                        100L,
                        EndReason.ERROR,
                        null
                ),
                record(
                        LocalDateTime.of(
                                2026, 8, 28, 10, 0
                        ),
                        100L,
                        EndReason.NO_AUDIO,
                        null
                )
        );

        List<TrainingRecord> previousRecords = List.of(
                record(
                        LocalDateTime.of(
                                2026, 8, 18, 10, 0
                        ),
                        60L,
                        EndReason.SCENARIO_DONE,
                        analysis("80", "75", "70", true)
                )
        );

        when(trainingRecordRepository
                .findAllByUserAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtAsc(
                        user,
                        LocalDateTime.of(
                                2026, 8, 24, 0, 0
                        ),
                        LocalDateTime.of(
                                2026, 8, 31, 0, 0
                        )
                ))
                .thenReturn(currentRecords);

        when(trainingRecordRepository
                .findAllByUserAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtAsc(
                        user,
                        LocalDateTime.of(
                                2026, 8, 17, 0, 0
                        ),
                        LocalDateTime.of(
                                2026, 8, 24, 0, 0
                        )
                ))
                .thenReturn(previousRecords);

        WeeklySummaryResponse response =
                dashboardService.getWeeklySummary();

        assertThat(response.trainingCount()).isEqualTo(2);
        assertThat(response.totalCallDurationSeconds())
                .isEqualTo(180L);

        assertThat(response.averageStabilityScore())
                .isEqualByComparingTo("90.00");

        assertThat(response.comment())
                .contains(
                        "안정도 점수가 지난주보다 "
                                + "10점 높아졌어요."
                );

        assertThat(response.comment())
                .contains(
                        "매끄러움 점수가 지난주보다 "
                                + "10점 낮아졌어요."
                );
    }

    @Test
    void returnsEmptySummaryWithoutTraining() {
        when(trainingRecordRepository
                .findAllByUserAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtAsc(
                        user,
                        LocalDateTime.of(
                                2026, 8, 24, 0, 0
                        ),
                        LocalDateTime.of(
                                2026, 8, 31, 0, 0
                        )
                ))
                .thenReturn(List.of());

        when(trainingRecordRepository
                .findAllByUserAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtAsc(
                        user,
                        LocalDateTime.of(
                                2026, 8, 17, 0, 0
                        ),
                        LocalDateTime.of(
                                2026, 8, 24, 0, 0
                        )
                ))
                .thenReturn(List.of());

        WeeklySummaryResponse response =
                dashboardService.getWeeklySummary();

        assertThat(response.trainingCount()).isZero();
        assertThat(response.totalCallDurationSeconds()).isZero();
        assertThat(response.averageStabilityScore()).isNull();
        assertThat(response.averageConversationScore()).isNull();
        assertThat(response.averageFluencyScore()).isNull();
    }

    private TrainingRecord record(
            LocalDateTime startedAt,
            long durationSeconds,
            EndReason endReason,
            TrainingAnalysisMetrics analysis
    ) {
        return TrainingRecord.builder()
                .user(user)
                .sessionId(UUID.randomUUID().toString())
                .scenarioId(1L)
                .scenarioName("음식점 예약")
                .sessionType(SessionType.SCENARIO)
                .endReason(endReason)
                .startedAt(startedAt)
                .endedAt(
                        startedAt.plusSeconds(durationSeconds)
                )
                .durationSeconds(durationSeconds)
                .analysis(analysis)
                .build();
    }

    private TrainingAnalysisMetrics analysis(
            String stability,
            String conversation,
            String fluency,
            boolean versionTwo
    ) {
        return TrainingAnalysisMetrics.from(
                new TrainingAnalysisRequest(
                        new BigDecimal(stability),
                        new BigDecimal(conversation),
                        new BigDecimal(fluency),
                        6000L,
                        1000L,
                        100L,
                        2,
                        0L,
                        4000L,
                        0,
                        0,
                        AnalysisQualityStatus.PASS,
                        null,
                        versionTwo
                                ? "SPEECH_ANALYZER_V2"
                                : "SPEECH_ANALYZER_V1",
                        versionTwo
                                ? "ANALYSIS_POLICY_V2"
                                : "ANALYSIS_POLICY_V1"
                )
        );
    }
}
