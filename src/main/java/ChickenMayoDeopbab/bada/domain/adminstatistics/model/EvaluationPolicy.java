package ChickenMayoDeopbab.bada.domain.adminstatistics.model;

import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public record EvaluationPolicy(
        WindowMode windowMode, Anchor anchor, LocalDateTime start, LocalDateTime end,
        Integer targetCount, Integer horizonDays, Integer baselineLookbackDays, Integer assessmentGraceDays,
        Grouping grouping, Integer earlyCount, Integer lateCount, Set<Metric> metrics,
        Set<SessionType> sessionTypes, Set<EndReason> endReasons,
        String difficulty, Long scenarioId, String scenarioVersion, String analyzerVersion,
        String analysisPolicyVersion, Boolean allowLegacyAssessments, Boolean matchTrainingConditions,
        Map<Metric, BigDecimal> thresholds, Participation participation, Integer maximumSubjectiveDelayMinutes
) {
    public enum WindowMode { FIXED_PERIOD, ELAPSED_DAYS, TRAINING_DAYS, TRAINING_SESSIONS }
    public enum Anchor { PARTICIPATION, FIRST_TRAINING, FIRST_ASSESSMENT }
    public enum Grouping { DAY, SESSION }
    public record Participation(Set<SessionType> sessionTypes, Set<EndReason> endReasons, Integer minDurationSeconds) {
        public Participation {
            sessionTypes = sessionTypes == null ? Set.of(SessionType.SCENARIO, SessionType.CUSTOM) : Set.copyOf(sessionTypes);
            endReasons = endReasons == null ? Set.of(EndReason.SCENARIO_DONE, EndReason.USER_END, EndReason.END_CALL, EndReason.TIMEOUT)
                    : Set.copyOf(endReasons);
            minDurationSeconds = minDurationSeconds == null ? 0 : minDurationSeconds;
            if (sessionTypes.isEmpty() || endReasons.isEmpty() || minDurationSeconds < 0 || minDurationSeconds > 3600) {
                throw new IllegalArgumentException("참여 인정 기준을 확인해 주세요.");
            }
        }
    }
    public enum Metric {
        SELF_ASSESSMENT(false), SUBJECTIVE_ANXIETY(false), STABILITY(true), CONVERSATION(true), FLUENCY(true);
        private final boolean higherIsBetter;
        Metric(boolean higherIsBetter) { this.higherIsBetter = higherIsBetter; }
        public boolean higherIsBetter() { return higherIsBetter; }
    }

    public EvaluationPolicy {
        windowMode = windowMode == null ? WindowMode.ELAPSED_DAYS : windowMode;
        anchor = anchor == null ? Anchor.PARTICIPATION : anchor;
        targetCount = targetCount == null ? 14 : targetCount;
        horizonDays = horizonDays == null ? 90 : horizonDays;
        baselineLookbackDays = baselineLookbackDays == null ? 7 : baselineLookbackDays;
        assessmentGraceDays = assessmentGraceDays == null ? 3 : assessmentGraceDays;
        grouping = grouping == null ? Grouping.DAY : grouping;
        earlyCount = earlyCount == null ? 3 : earlyCount;
        lateCount = lateCount == null ? 3 : lateCount;
        metrics = metrics == null ? Set.of(Metric.SELF_ASSESSMENT, Metric.SUBJECTIVE_ANXIETY) : Set.copyOf(metrics);
        sessionTypes = sessionTypes == null ? Set.of(SessionType.SCENARIO, SessionType.CUSTOM) : Set.copyOf(sessionTypes);
        endReasons = endReasons == null ? Set.of(EndReason.SCENARIO_DONE, EndReason.END_CALL, EndReason.TIMEOUT)
                : Set.copyOf(endReasons);
        allowLegacyAssessments = Boolean.TRUE.equals(allowLegacyAssessments);
        matchTrainingConditions = !Boolean.FALSE.equals(matchTrainingConditions);
        thresholds = thresholds == null ? Map.of() : Map.copyOf(thresholds);
        participation = participation == null ? new Participation(null, null, null) : participation;
        maximumSubjectiveDelayMinutes = maximumSubjectiveDelayMinutes == null ? 60 : maximumSubjectiveDelayMinutes;
        if (targetCount < 1 || targetCount > 365 || horizonDays < 1 || horizonDays > 730
                || baselineLookbackDays < 0 || baselineLookbackDays > 365
                || assessmentGraceDays < 0 || assessmentGraceDays > 90
                || earlyCount < 1 || lateCount < 1 || earlyCount > 100 || lateCount > 100
                || maximumSubjectiveDelayMinutes < 0 || maximumSubjectiveDelayMinutes > 10080
                || metrics.isEmpty() || sessionTypes.isEmpty() || endReasons.isEmpty()) {
            throw new IllegalArgumentException("평가 기간·자료 수·지표 설정을 확인해 주세요.");
        }
        if (windowMode == WindowMode.FIXED_PERIOD && (start == null || end == null || !end.isAfter(start))) {
            throw new IllegalArgumentException("고정 평가 기간의 시작과 종료가 필요합니다.");
        }
        if (windowMode == WindowMode.ELAPSED_DAYS && targetCount > horizonDays) {
            throw new IllegalArgumentException("평가 일수는 최대 관찰 기간 이하여야 합니다.");
        }
        for (var entry : thresholds.entrySet()) {
            Metric metric = entry.getKey();
            BigDecimal threshold = entry.getValue();
            BigDecimal maximum = metric == Metric.SELF_ASSESSMENT ? BigDecimal.valueOf(4)
                    : metric == Metric.SUBJECTIVE_ANXIETY ? BigDecimal.TEN : BigDecimal.valueOf(100);
            if (!metrics.contains(metric) || threshold.signum() < 0 || threshold.compareTo(maximum) > 0) {
                throw new IllegalArgumentException("지표의 변화량 기준이 유효하지 않습니다.");
            }
        }
    }
}
