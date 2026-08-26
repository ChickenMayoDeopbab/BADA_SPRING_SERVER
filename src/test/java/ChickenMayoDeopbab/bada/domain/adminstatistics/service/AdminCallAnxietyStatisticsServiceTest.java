package ChickenMayoDeopbab.bada.domain.adminstatistics.service;

import ChickenMayoDeopbab.bada.domain.adminstatistics.dto.response.CallAnxietySummaryResponse;
import ChickenMayoDeopbab.bada.domain.adminstatistics.model.AppliedTrainingStatisticsRow;
import ChickenMayoDeopbab.bada.domain.adminstatistics.model.CallAnxietyCsvExport;
import ChickenMayoDeopbab.bada.domain.adminstatistics.model.CallAnxietyStateStatisticsRow;
import ChickenMayoDeopbab.bada.domain.callanxiety.repository.CallAnxietyStateRepository;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;
import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminCallAnxietyStatisticsServiceTest {

    private CallAnxietyStateRepository
            callAnxietyStateRepository;

    private TrainingRecordRepository
            trainingRecordRepository;

    private AdminCallAnxietyStatisticsService service;

    @BeforeEach
    void setUp() {
        callAnxietyStateRepository =
                mock(CallAnxietyStateRepository.class);

        trainingRecordRepository =
                mock(TrainingRecordRepository.class);

        service = new AdminCallAnxietyStatisticsService(
                callAnxietyStateRepository,
                trainingRecordRepository,
                new ObjectMapper()
        );
    }

    @Test
    void calculatesSummaryUsingAllEligibleUsers() {
        when(
                callAnxietyStateRepository
                        .findAllForAdminStatistics()
        ).thenReturn(List.of(
                state(
                        1L,
                        "3.0000",
                        "2.6000",
                        CallPhobiaLevel.LEVEL_3,
                        CallPhobiaLevel.LEVEL_2,
                        3
                ),
                state(
                        2L,
                        "3.0000",
                        "3.1000",
                        CallPhobiaLevel.LEVEL_3,
                        CallPhobiaLevel.LEVEL_3,
                        3
                ),
                state(
                        3L,
                        "4.0000",
                        "4.0000",
                        CallPhobiaLevel.LEVEL_4,
                        CallPhobiaLevel.LEVEL_4,
                        2
                )
        ));

        when(
                trainingRecordRepository
                        .findAllAppliedForAdminStatistics()
        ).thenReturn(List.of(
                training(1L, 3L, "2.7000"),
                training(1L, 2L, "2.8000"),
                training(1L, 1L, "2.9000"),

                training(2L, 3L, "3.1000"),
                training(2L, 2L, "3.1000"),
                training(2L, 1L, "3.1000")
        ));

        CallAnxietySummaryResponse result =
                service.getSummary();

        assertThat(result.scope())
                .isEqualTo("ALL_TIME_CURRENT_SNAPSHOT");

        assertThat(result.scoringVersion())
                .isEqualTo("CALL_ANXIETY_V1");

        assertThat(result.totalUserCount())
                .isEqualTo(3);

        assertThat(result.eligibleUserCount())
                .isEqualTo(2);

        assertThat(result.improvedUserCount())
                .isEqualTo(1);

        assertThat(result.improvementRate())
                .isEqualByComparingTo("50.00");

        assertThat(result.averageInitialScore())
                .isEqualByComparingTo("3.00");

        assertThat(result.averageCurrentIndex())
                .isEqualByComparingTo("2.85");

        // (0.4 + -0.1) / 2 = 0.15
        // 개선 사용자만이 아니라 eligible 전체를 사용한다.
        assertThat(result.averageScoreChange())
                .isEqualByComparingTo("0.15");

        assertThat(result.levelImprovedUserCount())
                .isEqualTo(1);

        assertThat(result.levelImprovementRate())
                .isEqualByComparingTo("50.00");
    }

    @Test
    void returnsNullAveragesWhenNoUserIsEligible() {
        when(
                callAnxietyStateRepository
                        .findAllForAdminStatistics()
        ).thenReturn(List.of(
                state(
                        1L,
                        "3.0000",
                        "3.0000",
                        CallPhobiaLevel.LEVEL_3,
                        CallPhobiaLevel.LEVEL_3,
                        2
                )
        ));

        when(
                trainingRecordRepository
                        .findAllAppliedForAdminStatistics()
        ).thenReturn(List.of());

        CallAnxietySummaryResponse result =
                service.getSummary();

        assertThat(result.eligibleUserCount())
                .isZero();

        assertThat(result.improvementRate())
                .isNull();

        assertThat(result.averageInitialScore())
                .isNull();

        assertThat(result.averageCurrentIndex())
                .isNull();

        assertThat(result.averageScoreChange())
                .isNull();

        assertThat(result.levelImprovementRate())
                .isNull();
    }

    @Test
    void doesNotUseDifferentScoringVersionInRecentThree() {
        when(
                callAnxietyStateRepository
                        .findAllForAdminStatistics()
        ).thenReturn(List.of(
                state(
                        1L,
                        "3.0000",
                        "2.6000",
                        CallPhobiaLevel.LEVEL_3,
                        CallPhobiaLevel.LEVEL_2,
                        3
                )
        ));

        when(
                trainingRecordRepository
                        .findAllAppliedForAdminStatistics()
        ).thenReturn(List.of(
                training(
                        1L,
                        4L,
                        "1.0000",
                        "OLD_VERSION"
                ),
                training(1L, 3L, "2.7000"),
                training(1L, 2L, "2.8000"),
                training(1L, 1L, "2.9000")
        ));

        CallAnxietySummaryResponse result =
                service.getSummary();

        assertThat(result.improvedUserCount())
                .isEqualTo(1);
    }

    @Test
    void exportsUserIdWithoutNameOrEmail() {
        when(
                callAnxietyStateRepository
                        .findAllForAdminStatistics()
        ).thenReturn(List.of(
                state(
                        10L,
                        "3.0000",
                        "2.6000",
                        CallPhobiaLevel.LEVEL_3,
                        CallPhobiaLevel.LEVEL_2,
                        3
                )
        ));

        when(
                trainingRecordRepository
                        .findAllAppliedForAdminStatistics()
        ).thenReturn(List.of(
                new AppliedTrainingStatisticsRow(
                        10L,
                        "HARD",
                        AiPersonality.RUDE,
                        "SPEECH_ANALYZER_V1",
                        "CALL_ANXIETY_V1",
                        new BigDecimal("2.7000"),
                        3L
                ),
                new AppliedTrainingStatisticsRow(
                        10L,
                        "MEDIUM",
                        AiPersonality.KIND,
                        "SPEECH_ANALYZER_V1",
                        "CALL_ANXIETY_V1",
                        new BigDecimal("2.8000"),
                        2L
                ),
                new AppliedTrainingStatisticsRow(
                        10L,
                        "HARD",
                        AiPersonality.RUDE,
                        "SPEECH_ANALYZER_V1",
                        "CALL_ANXIETY_V1",
                        new BigDecimal("2.9000"),
                        1L
                )
        ));

        CallAnxietyCsvExport export =
                service.exportCsv();

        String csv = new String(
                export.content(),
                StandardCharsets.UTF_8
        );

        assertThat(csv).startsWith(
                "\uFEFFuserId,initialSelfReportScore"
        );

        assertThat(csv).contains(
                "\"{\"\"HARD\"\":2,\"\"MEDIUM\"\":1}\""
        );

        assertThat(csv).contains(
                "\"{\"\"KIND\"\":1,\"\"RUDE\"\":2}\""
        );

        assertThat(csv).doesNotContain("name");
        assertThat(csv).doesNotContain("email");

        assertThat(export.fileName())
                .startsWith("call-anxiety-statistics-")
                .endsWith(".csv");
    }

    private CallAnxietyStateStatisticsRow state(
            Long userId,
            String initialScore,
            String currentScore,
            CallPhobiaLevel initialLevel,
            CallPhobiaLevel currentLevel,
            int validTrainingCount
    ) {
        return new CallAnxietyStateStatisticsRow(
                userId,
                new BigDecimal(initialScore),
                new BigDecimal(currentScore),
                initialLevel,
                currentLevel,
                validTrainingCount,
                "CALL_ANXIETY_V1"
        );
    }

    private AppliedTrainingStatisticsRow training(
            Long userId,
            Long scoreSequence,
            String trainingStateIndex
    ) {
        return training(
                userId,
                scoreSequence,
                trainingStateIndex,
                "CALL_ANXIETY_V1"
        );
    }

    private AppliedTrainingStatisticsRow training(
            Long userId,
            Long scoreSequence,
            String trainingStateIndex,
            String scoringVersion
    ) {
        return new AppliedTrainingStatisticsRow(
                userId,
                "MEDIUM",
                AiPersonality.NORMAL,
                "SPEECH_ANALYZER_V1",
                scoringVersion,
                new BigDecimal(trainingStateIndex),
                scoreSequence
        );
    }
}
