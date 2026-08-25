package ChickenMayoDeopbab.bada.domain.adminstatistics.service;

import ChickenMayoDeopbab.bada.domain.adminstatistics.dto.response.CallAnxietySummaryResponse;
import ChickenMayoDeopbab.bada.domain.adminstatistics.model.AppliedTrainingStatisticsRow;
import ChickenMayoDeopbab.bada.domain.adminstatistics.model.CallAnxietyCsvExport;
import ChickenMayoDeopbab.bada.domain.adminstatistics.model.CallAnxietyStateStatisticsRow;
import ChickenMayoDeopbab.bada.domain.callanxiety.entity.CallAnxietyState;
import ChickenMayoDeopbab.bada.domain.callanxiety.repository.CallAnxietyStateRepository;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCallAnxietyStatisticsService {
    private static final String SCOPE = "ALL_TIME_CURRENT_SNAPSHOT";
    private static final ZoneId STATISTICS_ZONE = ZoneId.of("Asia/Seoul");
    private static final int MIN_VALID_TRAINING_COUNT = 3;
    private static final BigDecimal MIN_IMPROVEMENT = new BigDecimal("0.20");
    private static final int SCORE_SCALE = 4;
    private static final int SUMMARY_SCALE = 2;
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final CallAnxietyStateRepository callAnxietyStateRepository;
    private final TrainingRecordRepository trainingRecordRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public CallAnxietySummaryResponse getSummary() {
        OffsetDateTime generatedAt = currentTime();

        List<UserStatistics> userStatistics =
                loadUserStatistics();

        long totalUserCount = userStatistics.size();

        List<UserStatistics> eligibleUsers =
                userStatistics.stream()
                        .filter(UserStatistics::eligible)
                        .toList();

        long eligibleUserCount = eligibleUsers.size();

        long improvedUserCount =
                eligibleUsers.stream()
                        .filter(UserStatistics::improved)
                        .count();

        long levelImprovedUserCount =
                eligibleUsers.stream()
                        .filter(UserStatistics::levelImproved)
                        .count();

        BigDecimal improvementRate =
                calculateImprovementRate(
                        improvedUserCount,
                        eligibleUserCount
                );

        BigDecimal averageInitialScore =
                calculateAverageScore(
                        eligibleUsers,
                        statistics -> statistics
                                .state()
                                .initialSelfReportScore()
                );

        BigDecimal averageCurrentIndex =
                calculateAverageScore(
                        eligibleUsers,
                        statistics -> statistics
                                .state()
                                .currentCallAnxietyIndex()
                );

        BigDecimal averageScoreChange =
                calculateAverageScore(
                        eligibleUsers,
                        UserStatistics::scoreChange
                );

        BigDecimal levelImprovementRate =
                calculateImprovementRate(
                        levelImprovedUserCount,
                        eligibleUserCount
                );

        return new CallAnxietySummaryResponse(
                generatedAt,
                SCOPE,
                CallAnxietyState.SCORING_VERSION,
                totalUserCount,
                eligibleUserCount,
                improvedUserCount,
                improvementRate,
                averageInitialScore,
                averageCurrentIndex,
                averageScoreChange,
                levelImprovedUserCount,
                levelImprovementRate
        );
    }

    @Transactional(readOnly = true)
    public CallAnxietyCsvExport exportCsv() {
        OffsetDateTime generatedAt = currentTime();

        List<UserStatistics> userStatistics =
                loadUserStatistics();

        StringBuilder csv = new StringBuilder();

        appendCsvRow(
                csv,
                "userId",
                "initialSelfReportScore",
                "currentCallAnxietyIndex",
                "scoreChange",
                "initialLevel",
                "currentLevel",
                "validTrainingCount",
                "recentThreeAverage",
                "improved",
                "difficultyDistribution",
                "personalityDistribution",
                "analyzerVersions",
                "scoringVersions"
        );

        for (UserStatistics statistics : userStatistics) {
            CallAnxietyStateStatisticsRow state =
                    statistics.state();

            appendCsvRow(
                    csv,
                    String.valueOf(state.userId()),
                    formatScore(state.initialSelfReportScore()),
                    formatScore(state.currentCallAnxietyIndex()),
                    formatScore(statistics.scoreChange()),
                    state.initialLevel().name(),
                    state.currentLevel().name(),
                    String.valueOf(state.validTrainingCount()),
                    formatScore(statistics.recentThreeAverage()),
                    String.valueOf(statistics.improved()),
                    toJson(
                            difficultyDistribution(
                                    statistics.appliedTrainings()
                            )
                    ),
                    toJson(
                            personalityDistribution(
                                    statistics.appliedTrainings()
                            )
                    ),
                    toJson(
                            collectVersions(
                                    statistics.appliedTrainings(),
                                    AppliedTrainingStatisticsRow::analyzerVersion
                            )
                    ),
                    toJson(
                            collectVersions(
                                    statistics.appliedTrainings(),
                                    AppliedTrainingStatisticsRow::scoringVersion
                            )
                    )
            );
        }

        String fileName =
                "call-anxiety-statistics-"
                        + generatedAt.format(FILE_TIME_FORMAT)
                        + ".csv";

        byte[] content =
                ("\uFEFF" + csv)
                        .getBytes(StandardCharsets.UTF_8);

        return new CallAnxietyCsvExport(
                fileName,
                content
        );
    }

    private List<UserStatistics> loadUserStatistics() {
        List<CallAnxietyStateStatisticsRow> states =
                callAnxietyStateRepository
                        .findAllForAdminStatistics();

        List<AppliedTrainingStatisticsRow> appliedTrainings =
                trainingRecordRepository
                        .findAllAppliedForAdminStatistics();

        Map<Long, List<AppliedTrainingStatisticsRow>>
                trainingsByUser =
                appliedTrainings.stream()
                        .collect(Collectors.groupingBy(
                                AppliedTrainingStatisticsRow::userId
                        ));

        List<UserStatistics> result = new ArrayList<>();

        for (CallAnxietyStateStatisticsRow state : states) {
            List<AppliedTrainingStatisticsRow> userTrainings =
                    trainingsByUser.getOrDefault(
                            state.userId(),
                            List.of()
                    );

            BigDecimal scoreChange =
                    normalizeScore(
                            state.initialSelfReportScore()
                                    .subtract(
                                            state.currentCallAnxietyIndex()
                                    )
                    );

            BigDecimal recentThreeAverage =
                    calculateRecentThreeAverage(
                            state,
                            userTrainings
                    );

            boolean eligible =
                    state.validTrainingCount()
                            >= MIN_VALID_TRAINING_COUNT;

            boolean improved =
                    eligible
                            && scoreChange.compareTo(
                            MIN_IMPROVEMENT
                    ) >= 0
                            && recentThreeAverage != null
                            && recentThreeAverage.compareTo(
                            state.initialSelfReportScore()
                                    .subtract(
                                            MIN_IMPROVEMENT
                                    )
                    ) <= 0;

            boolean levelImproved =
                    levelRank(state.currentLevel())
                            < levelRank(state.initialLevel());

            result.add(new UserStatistics(
                    state,
                    List.copyOf(userTrainings),
                    scoreChange,
                    recentThreeAverage,
                    eligible,
                    improved,
                    levelImproved
            ));
        }

        result.sort(Comparator.comparing(
                statistics -> statistics.state().userId()
        ));

        return result;
    }

    private BigDecimal calculateRecentThreeAverage(
            CallAnxietyStateStatisticsRow state,
            List<AppliedTrainingStatisticsRow> trainings
    ) {
        List<BigDecimal> recentIndexes =
                trainings.stream()
                        .filter(training ->
                                Objects.equals(
                                        training.scoringVersion(),
                                        state.scoringVersion()
                                )
                        )
                        .filter(training ->
                                training.scoreSequence() != null
                                        && training
                                        .trainingStateIndex() != null
                        )
                        .sorted(
                                Comparator.comparing(
                                        AppliedTrainingStatisticsRow
                                                ::scoreSequence
                                ).reversed()
                        )
                        .limit(3)
                        .map(
                                AppliedTrainingStatisticsRow
                                        ::trainingStateIndex
                        )
                        .toList();

        if (recentIndexes.size() < 3) {
            return null;
        }

        BigDecimal total =
                recentIndexes.stream()
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return total.divide(
                BigDecimal.valueOf(3),
                SCORE_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal calculateImprovementRate(
            long improvedUserCount,
            long eligibleUserCount
    ) {
        if (eligibleUserCount == 0) {
            return null;
        }

        return BigDecimal.valueOf(improvedUserCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(eligibleUserCount),
                        SUMMARY_SCALE,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal calculateAverageScore(
            List<UserStatistics> eligibleUsers,
            Function<UserStatistics, BigDecimal> scoreExtractor
    ) {
        if (eligibleUsers.isEmpty()) {
            return null;
        }

        BigDecimal total =
                eligibleUsers.stream()
                        .map(scoreExtractor)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return total.divide(
                BigDecimal.valueOf(eligibleUsers.size()),
                SUMMARY_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private Map<String, Long> difficultyDistribution(
            List<AppliedTrainingStatisticsRow> trainings
    ) {
        Map<String, Long> distribution = new TreeMap<>();

        for (AppliedTrainingStatisticsRow training : trainings) {
            String difficulty =
                    normalizeCondition(training.difficulty());

            distribution.merge(
                    difficulty,
                    1L,
                    Long::sum
            );
        }

        return distribution;
    }

    private Map<String, Long> personalityDistribution(
            List<AppliedTrainingStatisticsRow> trainings
    ) {
        Map<String, Long> distribution = new TreeMap<>();

        for (AppliedTrainingStatisticsRow training : trainings) {
            String personality =
                    training.personality() == null
                            ? "UNKNOWN"
                            : training.personality().name();

            distribution.merge(
                    personality,
                    1L,
                    Long::sum
            );
        }

        return distribution;
    }

    private TreeSet<String> collectVersions(
            List<AppliedTrainingStatisticsRow> trainings,
            Function<AppliedTrainingStatisticsRow, String> extractor
    ) {
        TreeSet<String> versions = new TreeSet<>();

        for (AppliedTrainingStatisticsRow training : trainings) {
            String version = extractor.apply(training);

            if (version != null && !version.isBlank()) {
                versions.add(version);
            }
        }

        return versions;
    }

    private String normalizeCondition(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }

        return value.trim();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "CSV 통계 필드를 JSON으로 변환하지 못했습니다.",
                    exception
            );
        }
    }

    private void appendCsvRow(
            StringBuilder csv,
            String... values
    ) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                csv.append(',');
            }

            csv.append(escapeCsv(values[index]));
        }

        csv.append("\r\n");
    }

    private String escapeCsv(String value) {
        String normalized =
                value == null ? "" : value;

        boolean requiresQuote =
                normalized.contains(",")
                        || normalized.contains("\"")
                        || normalized.contains("\n")
                        || normalized.contains("\r");

        if (!requiresQuote) {
            return normalized;
        }

        return "\""
                + normalized.replace("\"", "\"\"")
                + "\"";
    }

    private String formatScore(BigDecimal score) {
        if (score == null) {
            return "";
        }

        return normalizeScore(score).toPlainString();
    }

    private BigDecimal normalizeScore(BigDecimal score) {
        return score.setScale(
                SCORE_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private int levelRank(CallPhobiaLevel level) {
        return switch (level) {
            case LEVEL_1 -> 1;
            case LEVEL_2 -> 2;
            case LEVEL_3 -> 3;
            case LEVEL_4 -> 4;
            case LEVEL_5 -> 5;
        };
    }

    private OffsetDateTime currentTime() {
        return OffsetDateTime.now(STATISTICS_ZONE)
                .withNano(0);
    }

    private record UserStatistics(
            CallAnxietyStateStatisticsRow state,
            List<AppliedTrainingStatisticsRow> appliedTrainings,
            BigDecimal scoreChange,
            BigDecimal recentThreeAverage,
            boolean eligible,
            boolean improved,
            boolean levelImproved
    ) {
    }
}
