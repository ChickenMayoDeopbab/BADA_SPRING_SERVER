package ChickenMayoDeopbab.bada.domain.adminstatistics.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record EvaluationResult(Long userId, Set<String> sources, LocalDateTime start, LocalDateTime end,
        int trainingSessions, int trainingDays, List<Long> trainingRecordIds, String comparisonWindow,
        Map<EvaluationPolicy.Metric, MetricResult> metrics) {
    public record MetricResult(String status, String reason, BigDecimal before, BigDecimal after,
            BigDecimal change, String direction, Boolean meetsThreshold, int beforeCount, int afterCount,
            List<Long> sourceIds, Set<String> versions, LocalDateTime beforeAt, LocalDateTime afterAt) {
        public static MetricResult unavailable(String reason) {
            return new MetricResult("UNAVAILABLE", reason, null, null, null, null, null,
                    0, 0, List.of(), Set.of(), null, null);
        }
        public MetricResult unavailableBecause(String reason) {
            return new MetricResult("UNAVAILABLE", reason, before, after, null, null, null,
                    beforeCount, afterCount, sourceIds, versions, beforeAt, afterAt);
        }
    }
    public record MetricSummary(long evaluableUsers, long decreasedUsers, long unchangedUsers, long increasedUsers,
            long improvedUsers, Long thresholdMetUsers, BigDecimal improvementRate,
            BigDecimal meanBefore, BigDecimal meanAfter, BigDecimal meanChange,
            BigDecimal medianChange, BigDecimal ci95Lower, BigDecimal ci95Upper,
            boolean comparable, Map<String, Long> unavailableReasons, Map<String, MetricSummary> strata) {}
    public record Summary(long selectedUsers, long trainedUsers, long tenDayCompleters,
            long baselineRecordedUsers, long postRecordedUsers,
            Map<String, Long> sourceCounts, Map<EvaluationPolicy.Metric, MetricSummary> metrics) {}
}
