package ChickenMayoDeopbab.bada.domain.dashboard.service;

import ChickenMayoDeopbab.bada.domain.callanxiety.repository.CallAnxietyStateRepository;
import ChickenMayoDeopbab.bada.domain.dashboard.dto.response.CallPhobiaLevelResponse;
import ChickenMayoDeopbab.bada.domain.dashboard.dto.response.DashboardMetricsResponse;
import ChickenMayoDeopbab.bada.domain.dashboard.dto.response.WeeklySummaryResponse;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingAnalysisMetrics;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    private static final String ANALYZER_VERSION = "SPEECH_ANALYZER_V2";
    private static final String ANALYSIS_POLICY_VERSION = "ANALYSIS_POLICY_V2";
    private static final BigDecimal CHANGE_THRESHOLD = new BigDecimal("3.00");
    private static final Set<EndReason> UNTRAINED_END_REASONS = EnumSet.of(EndReason.ERROR, EndReason.NO_AUDIO);
    private final TrainingRecordRepository trainingRecordRepository;
    private final CallAnxietyStateRepository callAnxietyStateRepository;
    private final UsersRepository usersRepository;
    private final Clock dashboardClock;

    public DashboardMetricsResponse getWeeklyMetrics() {
        Users user = getUserInfo();
        WeekRange week = currentWeek();

        List<TrainingRecord> records =
                findRecords(user, week);

        List<LocalDate> dates = new ArrayList<>(7);
        List<BigDecimal> stabilityScores =
                new ArrayList<>(7);
        List<BigDecimal> conversationScores =
                new ArrayList<>(7);
        List<BigDecimal> fluencyScores =
                new ArrayList<>(7);

        for (int offset = 0; offset < 7; offset++) {
            LocalDate date = week.start().plusDays(offset);

            MetricAverages dailyAverage = averageMetrics(
                    records.stream()
                            .filter(record ->
                                    record.getStartedAt()
                                            .toLocalDate()
                                            .equals(date)
                            )
                            .toList()
            );

            dates.add(date);
            stabilityScores.add(dailyAverage.stability());
            conversationScores.add(dailyAverage.conversation());
            fluencyScores.add(dailyAverage.fluency());
        }

        CallPhobiaLevelResponse level =
                callAnxietyStateRepository
                        .findByUser(user)
                        .map(state ->
                                CallPhobiaLevelResponse.from(
                                        state.getCurrentLevel()
                                )
                        )
                        .orElse(null);

        return new DashboardMetricsResponse(
                week.start(),
                week.endInclusive(),
                dates,
                stabilityScores,
                conversationScores,
                fluencyScores,
                level
        );
    }

    public WeeklySummaryResponse getWeeklySummary() {
        Users user = getUserInfo();

        WeekRange currentWeek = currentWeek();
        WeekRange previousWeek = currentWeek.previous();

        List<TrainingRecord> currentRecords =
                findRecords(user, currentWeek);

        List<TrainingRecord> previousRecords =
                findRecords(user, previousWeek);

        List<TrainingRecord> trainedRecords =
                currentRecords.stream()
                        .filter(this::isTrainingRecord)
                        .toList();

        long trainingCount = trainedRecords.size();

        long totalCallDurationSeconds =
                trainedRecords.stream()
                        .map(TrainingRecord::getDurationSeconds)
                        .filter(duration ->
                                duration != null && duration > 0
                        )
                        .mapToLong(Long::longValue)
                        .sum();

        MetricAverages currentAverage =
                averageMetrics(currentRecords);

        MetricAverages previousAverage =
                averageMetrics(previousRecords);

        return new WeeklySummaryResponse(
                currentWeek.start(),
                currentWeek.endInclusive(),
                trainingCount,
                totalCallDurationSeconds,
                currentAverage.stability(),
                currentAverage.conversation(),
                currentAverage.fluency(),
                buildComment(
                        trainingCount,
                        currentAverage,
                        previousAverage
                )
        );
    }

    private List<TrainingRecord> findRecords(
            Users user,
            WeekRange week
    ) {
        return trainingRecordRepository
                .findAllByUserAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtAsc(
                        user,
                        week.startDateTime(),
                        week.endExclusiveDateTime()
                );
    }

    private MetricAverages averageMetrics(
            List<TrainingRecord> records
    ) {
        List<TrainingAnalysisMetrics> analyses =
                records.stream()
                        .map(TrainingRecord::getAnalysis)
                        .filter(this::isV2PassedAnalysis)
                        .toList();

        if (analyses.isEmpty()) {
            return MetricAverages.empty();
        }

        BigDecimal count =
                BigDecimal.valueOf(analyses.size());

        return new MetricAverages(
                average(
                        analyses.stream()
                                .map(TrainingAnalysisMetrics::getStabilityScore)
                                .toList(),
                        count
                ),
                average(
                        analyses.stream()
                                .map(TrainingAnalysisMetrics::getConversationScore)
                                .toList(),
                        count
                ),
                average(
                        analyses.stream()
                                .map(TrainingAnalysisMetrics::getFluencyScore)
                                .toList(),
                        count
                )
        );
    }

    private BigDecimal average(
            List<BigDecimal> values,
            BigDecimal count
    ) {
        return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(
                        count,
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private boolean isV2PassedAnalysis(
            TrainingAnalysisMetrics analysis
    ) {
        return analysis != null
                && analysis.isPassed()
                && analysis.hasValidObjectiveScores()
                && ANALYZER_VERSION.equals(
                analysis.getAnalyzerVersion()
        )
                && ANALYSIS_POLICY_VERSION.equals(
                analysis.getAnalysisPolicyVersion()
        );
    }

    private boolean isTrainingRecord(
            TrainingRecord record
    ) {
        return record.getEndReason() != null
                && !UNTRAINED_END_REASONS.contains(
                record.getEndReason()
        );
    }

    private String buildComment(
            long trainingCount,
            MetricAverages current,
            MetricAverages previous
    ) {
        if (trainingCount == 0) {
            return "이번 주에는 아직 훈련 기록이 없어요. "
                    + "짧은 전화 시나리오부터 시작해보세요.";
        }

        String prefix = "이번 주 %d회 훈련했어요."
                .formatted(trainingCount);

        if (!current.hasAnyValue()) {
            return prefix
                    + " 분석 가능한 훈련이 쌓이면 "
                    + "지표 변화를 알려드릴게요.";
        }

        if (!previous.hasAnyValue()) {
            return prefix
                    + " 첫 주간 지표가 만들어졌어요. "
                    + "다음 주에도 이어가 보세요.";
        }

        List<MetricChange> changes =
                metricChanges(current, previous);

        if (changes.isEmpty()) {
            return prefix
                    + " 지난주와 비교 가능한 지표가 부족해요.";
        }

        MetricChange improvement = changes.stream()
                .filter(change ->
                        change.delta().compareTo(
                                CHANGE_THRESHOLD
                        ) >= 0
                )
                .max(Comparator.comparing(MetricChange::delta))
                .orElse(null);

        MetricChange decline = changes.stream()
                .filter(change ->
                        change.delta().compareTo(
                                CHANGE_THRESHOLD.negate()
                        ) <= 0
                )
                .min(Comparator.comparing(MetricChange::delta))
                .orElse(null);

        if (improvement == null && decline == null) {
            return prefix
                    + " 지난주와 비슷한 흐름을 유지하고 있어요.";
        }

        StringBuilder comment =
                new StringBuilder(prefix);

        if (improvement != null) {
            comment.append(" ")
                    .append(improvement.label())
                    .append(" 점수가 지난주보다 ")
                    .append(formatDelta(improvement.delta()))
                    .append("점 높아졌어요.");
        }

        if (decline != null) {
            comment.append(" ")
                    .append(decline.label())
                    .append(" 점수가 지난주보다 ")
                    .append(formatDelta(decline.delta().abs()))
                    .append("점 낮아졌어요. ")
                    .append(decline.advice());
        }

        return comment.toString();
    }

    private List<MetricChange> metricChanges(
            MetricAverages current,
            MetricAverages previous
    ) {
        List<MetricChange> changes =
                new ArrayList<>(3);

        addChange(
                changes,
                "안정도",
                current.stability(),
                previous.stability(),
                "말하기 전에 호흡을 고르고 "
                        + "천천히 시작해보세요."
        );

        addChange(
                changes,
                "대화 유지",
                current.conversation(),
                previous.conversation(),
                "통화 목적과 질문을 한 문장으로 "
                        + "미리 정리해보세요."
        );

        addChange(
                changes,
                "매끄러움",
                current.fluency(),
                previous.fluency(),
                "말의 속도를 낮추고 문장을 "
                        + "짧게 끊어 말해보세요."
        );

        return changes;
    }

    private void addChange(
            List<MetricChange> changes,
            String label,
            BigDecimal current,
            BigDecimal previous,
            String advice
    ) {
        if (current == null || previous == null) {
            return;
        }

        changes.add(
                new MetricChange(
                        label,
                        current.subtract(previous),
                        advice
                )
        );
    }

    private String formatDelta(BigDecimal delta) {
        return delta
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private WeekRange currentWeek() {
        LocalDate today =
                LocalDate.now(dashboardClock);

        LocalDate monday = today.with(
                TemporalAdjusters.previousOrSame(
                        DayOfWeek.MONDAY
                )
        );

        return WeekRange.fromMonday(monday);
    }

    private Users getUserInfo() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        return usersRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new ApplicationException(UsersStatusCode.USER_NOT_FOUND)
                );
    }

    private record MetricAverages(
            BigDecimal stability,
            BigDecimal conversation,
            BigDecimal fluency
    ) {
        private static MetricAverages empty() {
            return new MetricAverages(
                    null,
                    null,
                    null
            );
        }

        private boolean hasAnyValue() {
            return stability != null
                    || conversation != null
                    || fluency != null;
        }
    }

    private record MetricChange(
            String label,
            BigDecimal delta,
            String advice
    ) {
    }

    private record WeekRange(
            LocalDate start,
            LocalDate endInclusive
    ) {
        private static WeekRange fromMonday(
                LocalDate monday
        ) {
            return new WeekRange(
                    monday,
                    monday.plusDays(6)
            );
        }

        private WeekRange previous() {
            return fromMonday(start.minusWeeks(1));
        }

        private LocalDateTime startDateTime() {
            return start.atStartOfDay();
        }

        private LocalDateTime endExclusiveDateTime() {
            return endInclusive
                    .plusDays(1)
                    .atStartOfDay();
        }
    }
}
