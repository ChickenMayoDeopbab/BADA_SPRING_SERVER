package ChickenMayoDeopbab.bada.domain.dashboard.service;

import ChickenMayoDeopbab.bada.domain.dashboard.dto.request.DashboardQuery.DateRange;
import ChickenMayoDeopbab.bada.domain.dashboard.dto.request.DashboardQuery.Interval;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.projection.TrainingDashboardRow;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static ChickenMayoDeopbab.bada.domain.dashboard.dto.response.DashboardAnalyticsResponse.*;

/** 세 조회 API가 공유하는 활동/측정/비교 정책. 저장된 점수나 불안 상태를 변경하지 않는다. */
@Component
public class DashboardAggregator {
    public static final String POLICY_VERSION = "DASHBOARD_V1";
    public static final Set<EndReason> ACTIVITY_END_REASONS = Set.of(
            EndReason.SCENARIO_DONE, EndReason.USER_END, EndReason.END_CALL, EndReason.TIMEOUT);
    private static final Set<EndReason> SPEECH_END_REASONS = Set.of(
            EndReason.SCENARIO_DONE, EndReason.END_CALL, EndReason.TIMEOUT);
    private static final int MIN_COMPARISON_SAMPLES = 3;

    public enum Metric {
        ANXIETY("selfReportedAnxiety", null, "SCORE_0_10", "POINT"),
        TREMOR("tremorTurnRate", "tremor_turn_rate", "PERCENT", "PERCENTAGE_POINT"),
        PAUSE("longPauseTurnRate", "long_pause_turn_rate", "PERCENT", "PERCENTAGE_POINT"),
        QUIET("quietDropTurnRate", "quiet_drop_turn_rate", "PERCENT", "PERCENTAGE_POINT"),
        FAST("fastRateTurnRate", "fast_rate_turn_rate", "PERCENT", "PERCENTAGE_POINT"),
        DELAY("responseDelayMs", "response_delay_ms", "MILLISECOND", "MILLISECOND");

        public final String key;
        private final String storageKey;
        private final String unit;
        private final String deltaUnit;

        Metric(String key, String storageKey, String unit, String deltaUnit) {
            this.key = key;
            this.storageKey = storageKey;
            this.unit = unit;
            this.deltaUnit = deltaUnit;
        }
    }

    public boolean isTraining(TrainingDashboardRow row) {
        return (row.sessionType() == SessionType.SCENARIO || row.sessionType() == SessionType.CUSTOM)
                && row.endReason() != null && ACTIVITY_END_REASONS.contains(row.endReason());
    }

    public List<TrainingDashboardRow> trainings(List<TrainingDashboardRow> rows) {
        return rows.stream().filter(this::isTraining).toList();
    }

    public Summary summary(List<TrainingDashboardRow> rows) {
        var training = trainings(rows);
        long duration = training.stream().mapToLong(row -> row.durationSeconds() == null
                ? 0 : Math.max(0, row.durationSeconds())).sum();
        long warmups = rows.stream().filter(row -> row.sessionType() == SessionType.WARMUP
                && row.endReason() != null && ACTIVITY_END_REASONS.contains(row.endReason())).count();
        return new Summary(training.size(), training.stream().map(row -> row.startedAt().toLocalDate()).distinct().count(),
                duration, warmups);
    }

    public Map<String, MetricValue> metrics(List<TrainingDashboardRow> rows) {
        Map<String, MetricValue> result = new LinkedHashMap<>();
        for (Metric metric : Metric.values()) result.put(metric.key, observed(trainings(rows), metric));
        return result;
    }

    public Map<String, Long> hintSettings(List<TrainingDashboardRow> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (String key : List.of("1", "2", "3", "UNKNOWN")) result.put(key, 0L);
        for (var row : trainings(rows)) {
            Integer level = row.scriptLevel();
            String key = level != null && level >= 1 && level <= 3 ? level.toString() : "UNKNOWN";
            result.merge(key, 1L, Long::sum);
        }
        return result;
    }

    public Condition condition(TrainingDashboardRow row) {
        return new Condition(row.sessionType(), row.scenarioId(), row.scenarioVersion(), row.difficulty(),
                row.aiPersonality(), row.scriptLevel());
    }

    private boolean knownCondition(Condition condition) {
        return condition.scenarioId() != null && condition.scenarioVersion() != null
                && !condition.scenarioVersion().isBlank() && condition.difficulty() != null
                && !condition.difficulty().isBlank() && condition.aiPersonality() != null
                && condition.scriptLevel() != null && condition.scriptLevel() >= 1 && condition.scriptLevel() <= 3;
    }

    public List<ConditionCount> conditions(List<TrainingDashboardRow> rows) {
        Map<Condition, Long> counts = new LinkedHashMap<>();
        trainings(rows).forEach(row -> counts.merge(condition(row), 1L, Long::sum));
        return counts.entrySet().stream().map(entry -> new ConditionCount(entry.getKey(), entry.getValue())).toList();
    }

    private Sample sample(TrainingDashboardRow row, Metric metric) {
        if (metric == Metric.ANXIETY) {
            if (row.anxietyScore() == null) return Sample.excluded("MISSING_SELF_REPORT");
            if (row.anxietyScore() < 0 || row.anxietyScore() > 10) return Sample.excluded("INVALID_MEASUREMENT");
            return new Sample(row.anxietyScore().doubleValue(), 0, null);
        }
        if (row.endReason() == null || !SPEECH_END_REASONS.contains(row.endReason())) {
            return Sample.excluded("UNSUPPORTED_END_REASON");
        }
        var summary = row.speechMetrics();
        if (summary == null) return Sample.excluded("MISSING_SPEECH_METRICS");
        if (!summary.supportsCurrentPolicy()) return Sample.excluded("UNSUPPORTED_METRICS_VERSION");
        var measurement = summary.metrics() == null ? null : summary.metrics().get(metric.storageKey);
        if (measurement == null || !"MEASURED".equals(measurement.status())) {
            return Sample.excluded("UNAVAILABLE_METRIC");
        }
        if (metric == Metric.DELAY) {
            if (!measurement.hasValidDelay() || measurement.affectedTurnCount() != null) {
                return Sample.excluded("INVALID_MEASUREMENT");
            }
            return new Sample((double) measurement.totalDelayMs() / measurement.eligibleTurnCount(),
                    measurement.eligibleTurnCount(), null);
        }
        if (!measurement.hasValidRate() || measurement.totalDelayMs() != null
                || ((metric == Metric.QUIET || metric == Metric.FAST) && measurement.eligibleTurnCount() < 3)) {
            return Sample.excluded("INVALID_MEASUREMENT");
        }
        return new Sample(100.0 * measurement.affectedTurnCount() / measurement.eligibleTurnCount(),
                measurement.eligibleTurnCount(), null);
    }

    private MetricValue observed(List<TrainingDashboardRow> rows, Metric metric) {
        Map<String, Long> exclusions = new TreeMap<>();
        List<Sample> samples = new ArrayList<>();
        for (var row : rows) {
            var sample = sample(row, metric);
            if (sample.reason() == null) samples.add(sample);
            else exclusions.merge(sample.reason(), 1L, Long::sum);
        }
        String aggregation = metric == Metric.ANXIETY ? "MEAN_OF_SELF_REPORTS"
                : metric == Metric.DELAY ? "MEAN_OF_SESSION_MEANS" : "MEAN_OF_SESSION_RATES";
        return new MetricValue(samples.isEmpty() ? null : rounded(mean(samples)), metric.unit, aggregation,
                samples.size(), samples.stream().mapToLong(Sample::turns).sum(), status(samples.size()), exclusions);
    }

    public ComparisonData compare(List<TrainingDashboardRow> baselineRows, List<TrainingDashboardRow> currentRows) {
        var baseline = trainings(baselineRows);
        var current = trainings(currentRows);
        List<MetricComparison> metrics = new ArrayList<>();
        Map<String, Set<Long>> baselineIds = new LinkedHashMap<>();
        Map<String, Set<Long>> currentIds = new LinkedHashMap<>();
        for (Metric metric : Metric.values()) {
            MetricValue before = observed(baseline, metric);
            MetricValue after = observed(current, metric);
            Status overallStatus = pairStatus(before, after);
            // 반올림 전 평균으로 차이를 계산해 표시 자릿수에 따른 누적 오차를 피한다.
            BigDecimal delta = overallStatus == Status.READY
                    ? rounded(rawMean(current, metric) - rawMean(baseline, metric)) : null;
            var matched = matched(baseline, current, metric);
            metrics.add(new MetricComparison(metric.key, metric.unit, metric.deltaUnit,
                    new PeriodComparison(before, after, delta, overallStatus), matched.comparison()));
            baselineIds.put(metric.key, matched.baselineIds());
            currentIds.put(metric.key, matched.currentIds());
        }
        return new ComparisonData(metrics, baselineIds, currentIds);
    }

    private MatchedResult matched(List<TrainingDashboardRow> baseline, List<TrainingDashboardRow> current, Metric metric) {
        Map<String, Long> beforeExclusions = new TreeMap<>();
        Map<String, Long> afterExclusions = new TreeMap<>();
        var beforeGroups = eligibleGroups(baseline, metric, beforeExclusions);
        var afterGroups = eligibleGroups(current, metric, afterExclusions);
        Set<Condition> keys = new LinkedHashSet<>(beforeGroups.keySet());
        keys.addAll(afterGroups.keySet());
        Set<Long> beforeIds = new HashSet<>();
        Set<Long> afterIds = new HashSet<>();
        List<ConditionComparison> conditions = new ArrayList<>();
        long totalWeight = 0, beforeTurns = 0, afterTurns = 0;
        double beforeWeighted = 0, afterWeighted = 0;
        boolean shared = false;
        for (Condition key : keys) {
            var before = beforeGroups.getOrDefault(key, List.of());
            var after = afterGroups.getOrDefault(key, List.of());
            boolean common = !before.isEmpty() && !after.isEmpty();
            shared |= common;
            if (before.size() < MIN_COMPARISON_SAMPLES || after.size() < MIN_COMPARISON_SAMPLES) {
                String reason = common ? "LOW_SAMPLE_SIZE" : "NO_SHARED_CONDITION";
                if (!before.isEmpty()) beforeExclusions.merge(reason, (long) before.size(), Long::sum);
                if (!after.isEmpty()) afterExclusions.merge(reason, (long) after.size(), Long::sum);
                continue;
            }
            long weight = Math.min(before.size(), after.size());
            double beforeMean = rawMean(before, metric), afterMean = rawMean(after, metric);
            totalWeight += weight;
            beforeWeighted += beforeMean * weight;
            afterWeighted += afterMean * weight;
            for (var row : before) { beforeIds.add(row.recordId()); beforeTurns += sample(row, metric).turns(); }
            for (var row : after) { afterIds.add(row.recordId()); afterTurns += sample(row, metric).turns(); }
            conditions.add(new ConditionComparison(key, before.size(), after.size(), weight,
                    rounded(beforeMean), rounded(afterMean)));
        }
        Status status = totalWeight > 0 ? Status.READY : shared ? Status.INSUFFICIENT_DATA : Status.NOT_COMPARABLE;
        if (baseline.stream().noneMatch(row -> sample(row, metric).reason() == null)
                || current.stream().noneMatch(row -> sample(row, metric).reason() == null)) status = Status.NO_DATA;
        var before = new MetricValue(totalWeight == 0 ? null : rounded(beforeWeighted / totalWeight), metric.unit,
                "CONDITION_STANDARDIZED_MEAN", beforeIds.size(), beforeTurns, status, beforeExclusions);
        var after = new MetricValue(totalWeight == 0 ? null : rounded(afterWeighted / totalWeight), metric.unit,
                "CONDITION_STANDARDIZED_MEAN", afterIds.size(), afterTurns, status, afterExclusions);
        return new MatchedResult(new MatchedComparison(before, after,
                totalWeight == 0 ? null : rounded((afterWeighted - beforeWeighted) / totalWeight), status,
                conditions.size(), conditions), beforeIds, afterIds);
    }

    private Map<Condition, List<TrainingDashboardRow>> eligibleGroups(List<TrainingDashboardRow> rows, Metric metric,
                                                                    Map<String, Long> exclusions) {
        Map<Condition, List<TrainingDashboardRow>> groups = new LinkedHashMap<>();
        for (var row : rows) {
            String reason = sample(row, metric).reason();
            Condition key = condition(row);
            if (reason == null && !knownCondition(key)) reason = "UNKNOWN_CONDITION";
            if (reason != null) exclusions.merge(reason, 1L, Long::sum);
            else groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
        }
        return groups;
    }

    public List<SeriesPoint> series(List<TrainingDashboardRow> rows, DateRange range, Interval interval, LocalDate today) {
        return buckets(range, interval).stream().map(bucket -> {
            var included = rows.stream().filter(row -> bucket.contains(row.startedAt())).toList();
            return new SeriesPoint(bucket.from(), bucket.to(), partial(bucket, interval, today),
                    trainings(included).size(), metrics(included));
        }).toList();
    }

    public List<ComparisonSeriesPoint> comparisonSeries(List<TrainingDashboardRow> rows, DateRange range,
            Interval interval, LocalDate today, Map<String, Set<Long>> matchedIds) {
        return buckets(range, interval).stream().map(bucket -> {
            var included = trainings(rows).stream().filter(row -> bucket.contains(row.startedAt())).toList();
            Map<String, Long> counts = new LinkedHashMap<>();
            matchedIds.forEach((key, ids) -> counts.put(key, included.stream().filter(row -> ids.contains(row.recordId())).count()));
            return new ComparisonSeriesPoint(bucket.from(), bucket.to(), partial(bucket, interval, today),
                    included.size(), counts);
        }).toList();
    }

    public List<Activity> activity(List<TrainingDashboardRow> rows, DateRange range) {
        Map<LocalDate, Long> counts = new HashMap<>();
        trainings(rows).forEach(row -> counts.merge(row.startedAt().toLocalDate(), 1L, Long::sum));
        return range.from().datesUntil(range.to().plusDays(1))
                .map(date -> new Activity(date, counts.getOrDefault(date, 0L))).toList();
    }

    private List<DateRange> buckets(DateRange range, Interval interval) {
        List<DateRange> result = new ArrayList<>();
        LocalDate start = range.from();
        while (!start.isAfter(range.to())) {
            LocalDate end = interval == Interval.DAY ? start : start.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            if (end.isAfter(range.to())) end = range.to();
            result.add(new DateRange(start, end));
            start = end.plusDays(1);
        }
        return result;
    }

    private boolean partial(DateRange bucket, Interval interval, LocalDate today) {
        return !bucket.to().isBefore(today) || (interval == Interval.WEEK
                && (bucket.from().getDayOfWeek() != DayOfWeek.MONDAY || bucket.to().getDayOfWeek() != DayOfWeek.SUNDAY));
    }

    private double rawMean(List<TrainingDashboardRow> rows, Metric metric) {
        return mean(rows.stream().map(row -> sample(row, metric)).filter(sample -> sample.reason() == null).toList());
    }

    private double mean(List<Sample> samples) {
        return samples.stream().mapToDouble(Sample::value).average().orElse(0);
    }

    private Status status(long count) {
        return count == 0 ? Status.NO_DATA : count < MIN_COMPARISON_SAMPLES ? Status.INSUFFICIENT_DATA : Status.READY;
    }

    private Status pairStatus(MetricValue before, MetricValue after) {
        if (before.sampleCount() == 0 || after.sampleCount() == 0) return Status.NO_DATA;
        return before.status() == Status.READY && after.status() == Status.READY ? Status.READY : Status.INSUFFICIENT_DATA;
    }

    private BigDecimal rounded(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private record Sample(double value, long turns, String reason) {
        static Sample excluded(String reason) { return new Sample(0, 0, reason); }
    }
    private record MatchedResult(MatchedComparison comparison, Set<Long> baselineIds, Set<Long> currentIds) {}
    public record ComparisonData(List<MetricComparison> metrics, Map<String, Set<Long>> baselineIds,
                                 Map<String, Set<Long>> currentIds) {}
}
