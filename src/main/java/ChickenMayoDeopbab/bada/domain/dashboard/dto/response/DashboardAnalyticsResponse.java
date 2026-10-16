package ChickenMayoDeopbab.bada.domain.dashboard.dto.response;

import ChickenMayoDeopbab.bada.domain.dashboard.dto.request.DashboardQuery.Interval;
import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request.TrainingRecordFilter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class DashboardAnalyticsResponse {
    private DashboardAnalyticsResponse() {}

    public enum Status { READY, INSUFFICIENT_DATA, NO_DATA, NOT_COMPARABLE }

    public record Period(LocalDate from, LocalDate to, boolean isPartial) {}
    public record Meta(Period period, Period baseline, Interval interval, TrainingRecordFilter filters,
                       String timeZone, Instant generatedAt, String aggregationPolicyVersion) {}
    public record Summary(long trainingCount, long trainingDays, long totalDurationSeconds, long warmupCount) {}
    public record Experience(long scenarioCount, long newScenarioCount, long unknownScenarioTrainingCount) {}
    public record Coverage(long trainingCount, long selfReportCount, Map<String, Long> metricSampleCounts) {}

    // 값이 없다는 사실도 계약의 일부이므로 전역 Jackson 설정과 관계없이 null을 보존한다.
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record MetricValue(BigDecimal value, String unit, String aggregation, long sampleCount,
                              long eligibleTurnCount, Status status, Map<String, Long> exclusionReasons) {}
    public record SeriesPoint(LocalDate from, LocalDate to, boolean isPartial, long trainingCount,
                              Map<String, MetricValue> metrics) {}
    public record Activity(LocalDate date, long trainingCount) {}
    public record Overview(Meta meta, Summary summary, Experience experience, Map<String, Long> hintSettings,
                           Map<String, MetricValue> metrics, List<SeriesPoint> series, List<Activity> activity,
                           Coverage coverage) {}

    public record Condition(SessionType sessionType, Long scenarioId, String scenarioVersion, String difficulty,
                            AiPersonality aiPersonality, Integer scriptLevel) {}
    public record ConditionCount(Condition condition, long trainingCount) {}
    public record ConditionComparison(Condition condition, long baselineSampleCount, long currentSampleCount,
                                      long weight, BigDecimal baselineValue, BigDecimal currentValue) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record PeriodComparison(MetricValue baseline, MetricValue current, BigDecimal delta, Status status) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record MatchedComparison(MetricValue baseline, MetricValue current, BigDecimal delta, Status status,
                                    int conditionCount, List<ConditionComparison> conditions) {}
    public record MetricComparison(String key, String unit, String deltaUnit,
                                   PeriodComparison overall, MatchedComparison matched) {}
    public record ComparisonSeriesPoint(LocalDate from, LocalDate to, boolean isPartial, long trainingCount,
                                         Map<String, Long> matchedTrainingCounts) {}
    public record Comparison(Meta meta, Summary baselineSummary, Summary currentSummary,
                             List<MetricComparison> metrics, List<ConditionCount> baselineConditions,
                             List<ConditionCount> currentConditions, List<ComparisonSeriesPoint> baselineSeries,
                             List<ComparisonSeriesPoint> currentSeries) {}

    public record Scenario(Long scenarioId, SessionType sessionType, String scenarioName, Summary summary,
                           LocalDateTime firstTrainedAt, LocalDateTime lastTrainedAt,
                           List<ConditionCount> conditionBreakdown, Map<String, MetricValue> metrics,
                           List<MetricComparison> comparison, List<SeriesPoint> series) {}
    public record Scenarios(Meta meta, List<Scenario> items, int page, int size, long totalElements, int totalPages) {}
}
