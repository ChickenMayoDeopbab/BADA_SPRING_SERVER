package ChickenMayoDeopbab.bada.domain.adminstatistics.service;

import ChickenMayoDeopbab.bada.domain.adminstatistics.model.EvaluationPolicy;
import ChickenMayoDeopbab.bada.domain.adminstatistics.model.EvaluationResult;
import ChickenMayoDeopbab.bada.domain.adminstatistics.model.EvaluationResult.MetricResult;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisResult;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisType;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class EffectivenessCalculator {
    public record Participant(Long userId, Set<String> sources, LocalDateTime startedAt,
            LocalDateTime endOverride, Long baselineAssessmentId, Long postAssessmentId) {}
    private record Point(BigDecimal value, LocalDateTime at, List<Long> ids, Set<String> versions) {}

    public EvaluationResult evaluate(EvaluationPolicy policy, Participant participant, LocalDateTime asOf,
            List<DiagnosisResult> assessments, List<TrainingRecord> training) {
        List<DiagnosisResult> diagnoses = assessments.stream()
                .filter(result -> result.getType() == DiagnosisType.SIGNUP && !result.getCreatedAt().isAfter(asOf))
                .sorted(Comparator.comparing(DiagnosisResult::getCreatedAt).thenComparing(DiagnosisResult::getQuestionId))
                .toList();
        List<TrainingRecord> participation = training.stream().filter(record -> !record.getEndedAt().isAfter(asOf))
                .filter(record -> policy.participation().sessionTypes().contains(record.getSessionType()))
                .filter(record -> policy.participation().endReasons().contains(record.getEndReason()))
                .filter(record -> !record.getEndedAt().isBefore(record.getStartedAt())
                        && Duration.between(record.getStartedAt(), record.getEndedAt()).toSeconds() >= policy.participation().minDurationSeconds())
                .sorted(Comparator.comparing(TrainingRecord::getEndedAt).thenComparing(TrainingRecord::getRecordId)).toList();
        List<TrainingRecord> records = training.stream().filter(record -> !record.getEndedAt().isAfter(asOf))
                .filter(record -> policy.sessionTypes().contains(record.getSessionType()))
                .filter(record -> policy.endReasons().contains(record.getEndReason()))
                .filter(record -> policy.difficulty() == null || policy.difficulty().equals(record.getDifficulty()))
                .filter(record -> policy.scenarioId() == null || policy.scenarioId().equals(record.getScenarioId()))
                .filter(record -> policy.scenarioVersion() == null || policy.scenarioVersion().equals(record.getScenarioVersion()))
                .sorted(Comparator.comparing(TrainingRecord::getEndedAt).thenComparing(TrainingRecord::getRecordId))
                .toList();
        LocalDateTime start = policy.windowMode() == EvaluationPolicy.WindowMode.FIXED_PERIOD ? policy.start()
                : switch (policy.anchor()) {
                    case PARTICIPATION -> participant.startedAt();
                    case FIRST_TRAINING -> participation.isEmpty() ? null : participation.getFirst().getStartedAt();
                    case FIRST_ASSESSMENT -> diagnoses.isEmpty() ? null : diagnoses.getFirst().getCreatedAt();
                };
        Map<EvaluationPolicy.Metric, MetricResult> metrics = new EnumMap<>(EvaluationPolicy.Metric.class);
        if (start == null) {
            policy.metrics().forEach(metric -> metrics.put(metric, MetricResult.unavailable("MISSING_ANCHOR")));
            return new EvaluationResult(participant.userId(), participant.sources(), null, null, 0, 0, List.of(), "MISSING_ANCHOR", metrics);
        }
        LocalDateTime anchorStart = start;
        List<TrainingRecord> afterStart = participation.stream().filter(record -> !record.getStartedAt().isBefore(anchorStart))
                .filter(record -> !record.getEndedAt().isAfter(anchorStart.plusDays(policy.horizonDays()))).toList();
        LocalDateTime end = participant.endOverride() != null ? participant.endOverride()
                : resolveEnd(policy, start, afterStart);
        if (end != null && !end.isAfter(start)) { throw new IllegalArgumentException("개인별 종료는 시작 이후여야 합니다."); }
        LocalDateTime observationLimit = end == null ? start.plusDays(policy.horizonDays()) : end;
        LocalDateTime observedEnd = observationLimit.isAfter(asOf) ? asOf : observationLimit;
        List<TrainingRecord> observed = records.stream().filter(record -> !record.getStartedAt().isBefore(anchorStart))
                .filter(record -> !record.getEndedAt().isAfter(observedEnd)).toList();
        List<TrainingRecord> observedParticipation = participation.stream().filter(record -> !record.getStartedAt().isBefore(anchorStart))
                .filter(record -> !record.getEndedAt().isAfter(observedEnd)).toList();
        int days = (int) observedParticipation.stream().map(record -> record.getEndedAt().toLocalDate()).distinct().count();
        boolean notDue = end == null ? asOf.isBefore(start.plusDays(policy.horizonDays())) : end.isAfter(asOf);
        for (EvaluationPolicy.Metric metric : policy.metrics()) {
            MetricResult result;
            if (metric == EvaluationPolicy.Metric.SELF_ASSESSMENT) {
                // 판정 시점 전에도 수집된 검사 수는 보존해 응답률의 분모가 왜곡되지 않게 한다.
                result = end == null ? unavailableAssessment("MILESTONE_NOT_REACHED",
                        baselineAssessment(policy, participant, start, diagnoses), null)
                        : assessmentChange(policy, participant, start, end, asOf, diagnoses);
                if (notDue) { result = result.unavailableBecause("NOT_DUE"); }
            } else {
                result = notDue ? MetricResult.unavailable("NOT_DUE")
                        : end == null ? MetricResult.unavailable("MILESTONE_NOT_REACHED")
                        : trainingChange(policy, metric, observed, asOf);
            }
            metrics.put(metric, result);
        }
        Set<Long> usedRecordIds = new TreeSet<>();
        observed.forEach(record -> usedRecordIds.add(record.getRecordId()));
        observedParticipation.forEach(record -> usedRecordIds.add(record.getRecordId()));
        return new EvaluationResult(participant.userId(), participant.sources(), start, end, observedParticipation.size(), days,
                List.copyOf(usedRecordIds),
                policy.windowMode() + ":" + (end == null ? "UNREACHED"
                : participant.endOverride() != null || policy.windowMode() == EvaluationPolicy.WindowMode.FIXED_PERIOD
                || policy.windowMode() == EvaluationPolicy.WindowMode.ELAPSED_DAYS
                ? Duration.between(start, end).toMinutes() + "min" : policy.targetCount().toString()), Map.copyOf(metrics));
    }

    private LocalDateTime resolveEnd(EvaluationPolicy policy, LocalDateTime start, List<TrainingRecord> records) {
        return switch (policy.windowMode()) {
            case FIXED_PERIOD -> policy.end();
            case ELAPSED_DAYS -> start.plusDays(policy.targetCount());
            case TRAINING_SESSIONS -> records.size() < policy.targetCount() ? null
                    : records.get(policy.targetCount() - 1).getEndedAt();
            case TRAINING_DAYS -> {
                Set<LocalDate> days = new HashSet<>();
                LocalDateTime reached = null;
                for (TrainingRecord record : records) {
                    days.add(record.getEndedAt().toLocalDate());
                    if (days.size() >= policy.targetCount()) { reached = record.getEndedAt(); break; }
                }
                yield reached;
            }
        };
    }

    private MetricResult assessmentChange(EvaluationPolicy policy, Participant participant, LocalDateTime start,
            LocalDateTime end, LocalDateTime asOf, List<DiagnosisResult> diagnoses) {
        DiagnosisResult before = baselineAssessment(policy, participant, start, diagnoses);
        LocalDateTime afterFloor = before == null ? start : before.getCreatedAt();
        DiagnosisResult after = participant.postAssessmentId() != null
                ? diagnoses.stream().filter(result -> result.getQuestionId().equals(participant.postAssessmentId())).findFirst().orElse(null)
                : diagnoses.stream().filter(result -> result.getCreatedAt().isAfter(afterFloor)
                && !result.getCreatedAt().isBefore(end.minusDays(policy.assessmentGraceDays()))
                && !result.getCreatedAt().isAfter(end.plusDays(policy.assessmentGraceDays())))
                .min(Comparator.comparingLong((DiagnosisResult result) -> Math.abs(Duration.between(end, result.getCreatedAt()).toSeconds()))
                        .thenComparing(DiagnosisResult::getCreatedAt).thenComparing(DiagnosisResult::getQuestionId)).orElse(null);
        if (before == null) { return unavailableAssessment("MISSING_BASELINE", null, after); }
        if (before.getCreatedAt().isAfter(start)
                || before.getCreatedAt().isBefore(start.minusDays(policy.baselineLookbackDays()))) {
            return unavailableAssessment("BASELINE_OUTSIDE_WINDOW", before, after);
        }
        if (after == null) { return unavailableAssessment(asOf.isBefore(end.plusDays(policy.assessmentGraceDays()))
                ? "AWAITING_POST_ASSESSMENT" : "MISSING_POST_ASSESSMENT", before, null); }
        if (!after.getCreatedAt().isAfter(before.getCreatedAt())
                || after.getCreatedAt().isBefore(end.minusDays(policy.assessmentGraceDays()))
                || after.getCreatedAt().isAfter(end.plusDays(policy.assessmentGraceDays()))) {
            return unavailableAssessment("POST_OUTSIDE_WINDOW", before, after);
        }
        if (before.getScore() < 1 || before.getScore() > 5 || after.getScore() < 1 || after.getScore() > 5
                || !Double.isFinite(before.getScore()) || !Double.isFinite(after.getScore())) {
            return unavailableAssessment("INVALID_ASSESSMENT_SCORE", before, after);
        }
        boolean legacy = before.getQuestionnaireVersion() == null || after.getQuestionnaireVersion() == null
                || !Boolean.TRUE.equals(before.getQuestionnaireVersionConfirmed())
                || !Boolean.TRUE.equals(after.getQuestionnaireVersionConfirmed());
        if (legacy && !policy.allowLegacyAssessments()) { return unavailableAssessment("UNKNOWN_QUESTIONNAIRE_VERSION", before, after); }
        if (before.getQuestionnaireVersion() != null && after.getQuestionnaireVersion() != null
                && !before.getQuestionnaireVersion().equals(after.getQuestionnaireVersion())) {
            return unavailableAssessment("QUESTIONNAIRE_VERSION_MISMATCH", before, after);
        }
        String version = legacy ? "LEGACY_UNVERIFIED:" + Objects.toString(before.getQuestionnaireVersion(), "UNKNOWN")
                : before.getQuestionnaireVersion();
        return compared(policy, EvaluationPolicy.Metric.SELF_ASSESSMENT,
                new Point(BigDecimal.valueOf(before.getScore()), before.getCreatedAt(), List.of(before.getQuestionId()), Set.of(version)),
                new Point(BigDecimal.valueOf(after.getScore()), after.getCreatedAt(), List.of(after.getQuestionId()), Set.of(version)), 1, 1);
    }

    private DiagnosisResult baselineAssessment(EvaluationPolicy policy, Participant participant, LocalDateTime start,
            List<DiagnosisResult> diagnoses) {
        return participant.baselineAssessmentId() != null
                ? diagnoses.stream().filter(result -> result.getQuestionId().equals(participant.baselineAssessmentId())).findFirst().orElse(null)
                : diagnoses.stream().filter(result -> !result.getCreatedAt().isAfter(start)
                && !result.getCreatedAt().isBefore(start.minusDays(policy.baselineLookbackDays())))
                .max(Comparator.comparing(DiagnosisResult::getCreatedAt).thenComparing(DiagnosisResult::getQuestionId)).orElse(null);
    }

    private MetricResult unavailableAssessment(String reason, DiagnosisResult before, DiagnosisResult after) {
        List<Long> ids = new ArrayList<>();
        Set<String> versions = new TreeSet<>();
        for (DiagnosisResult result : Arrays.asList(before, after)) {
            if (result != null) { ids.add(result.getQuestionId()); versions.add(Objects.toString(result.getQuestionnaireVersion(), "UNKNOWN")); }
        }
        return new MetricResult("UNAVAILABLE", reason,
                before == null || !Double.isFinite(before.getScore()) ? null : BigDecimal.valueOf(before.getScore()),
                after == null || !Double.isFinite(after.getScore()) ? null : BigDecimal.valueOf(after.getScore()),
                null, null, null, before == null ? 0 : 1, after == null ? 0 : 1, ids, versions,
                before == null ? null : before.getCreatedAt(), after == null ? null : after.getCreatedAt());
    }

    private MetricResult trainingChange(EvaluationPolicy policy, EvaluationPolicy.Metric metric,
            List<TrainingRecord> records, LocalDateTime asOf) {
        List<TrainingRecord> usable = records.stream().filter(record -> value(policy, metric, record, asOf) != null)
                .filter(record -> metric == EvaluationPolicy.Metric.SUBJECTIVE_ANXIETY
                        || (record.getAnalysis() != null && record.getAnalysis().hasVersions()
                        && (policy.analyzerVersion() == null || policy.analyzerVersion().equals(record.getAnalysis().getAnalyzerVersion()))
                        && (policy.analysisPolicyVersion() == null || policy.analysisPolicyVersion().equals(record.getAnalysis().getAnalysisPolicyVersion()))))
                .toList();
        List<List<TrainingRecord>> groups = new ArrayList<>();
        if (policy.grouping() == EvaluationPolicy.Grouping.SESSION) {
            usable.forEach(record -> groups.add(List.of(record)));
        } else {
            Map<LocalDate, List<TrainingRecord>> days = usable.stream().collect(Collectors.groupingBy(
                    record -> record.getEndedAt().toLocalDate(), TreeMap::new, Collectors.toList()));
            groups.addAll(days.values());
        }
        if (groups.size() < policy.earlyCount() + policy.lateCount()) {
            return MetricResult.unavailable("INSUFFICIENT_NON_OVERLAPPING_OBSERVATIONS");
        }
        List<List<TrainingRecord>> early = groups.subList(0, policy.earlyCount());
        List<List<TrainingRecord>> late = groups.subList(groups.size() - policy.lateCount(), groups.size());
        List<TrainingRecord> comparedRecords = new ArrayList<>();
        early.forEach(comparedRecords::addAll);
        late.forEach(comparedRecords::addAll);
        if (policy.matchTrainingConditions()) {
            if (comparedRecords.stream().anyMatch(record -> record.getDifficulty() == null || record.getScriptLevel() == null
                    || (record.getScenarioId() != null && record.getScenarioVersion() == null))) {
                return MetricResult.unavailable("MISSING_TRAINING_CONDITIONS");
            }
            Set<String> conditions = comparedRecords.stream().map(record -> record.getSessionType() + ":"
                    + record.getScenarioId() + ":" + record.getScenarioVersion() + ":" + record.getDifficulty()
                    + ":" + record.getScriptLevel() + ":" + record.getAiPersonality()).collect(Collectors.toSet());
            if (conditions.size() != 1) { return MetricResult.unavailable("TRAINING_CONDITION_MISMATCH"); }
        }
        Point before = point(policy, metric, early, asOf);
        Point after = point(policy, metric, late, asOf);
        if (metric != EvaluationPolicy.Metric.SUBJECTIVE_ANXIETY
                && (before.versions().size() != 1 || !before.versions().equals(after.versions()))) {
            return MetricResult.unavailable("ANALYZER_VERSION_MISMATCH");
        }
        return compared(policy, metric, before, after, early.size(), late.size());
    }

    private BigDecimal value(EvaluationPolicy policy, EvaluationPolicy.Metric metric, TrainingRecord record, LocalDateTime asOf) {
        if (metric == EvaluationPolicy.Metric.SUBJECTIVE_ANXIETY) {
            LocalDateTime submittedAt = record.getAnxietyRecordedAt() != null ? record.getAnxietyRecordedAt() : record.getScoreAppliedAt();
            return record.getAnxietyScore() == null || record.getAnxietyScore() < 0 || record.getAnxietyScore() > 10
                    || submittedAt == null || submittedAt.isAfter(asOf)
                    || submittedAt.isBefore(record.getEndedAt())
                    || (policy.maximumSubjectiveDelayMinutes() > 0
                    && submittedAt.isAfter(record.getEndedAt().plusMinutes(policy.maximumSubjectiveDelayMinutes())))
                    ? null : BigDecimal.valueOf(record.getAnxietyScore());
        }
        if (record.getAnalysis() == null) { return null; }
        if (!record.getAnalysis().isMetricPassed(metric.name().toLowerCase(Locale.ROOT))) { return null; }
        BigDecimal value = switch (metric) {
            case STABILITY -> record.getAnalysis().getStabilityScore();
            case CONVERSATION -> record.getAnalysis().getConversationScore();
            case FLUENCY -> record.getAnalysis().getFluencyScore();
            default -> null;
        };
        return value == null || value.signum() < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0 ? null : value;
    }

    private Point point(EvaluationPolicy policy, EvaluationPolicy.Metric metric, List<List<TrainingRecord>> groups, LocalDateTime asOf) {
        List<TrainingRecord> records = groups.stream().flatMap(Collection::stream).toList();
        BigDecimal value = mean(groups.stream().map(group -> mean(group.stream()
                .map(record -> value(policy, metric, record, asOf)).toList())).toList());
        Set<String> versions = metric == EvaluationPolicy.Metric.SUBJECTIVE_ANXIETY ? Set.of("SUBJECTIVE_0_10_V1")
                : records.stream().map(record -> record.getAnalysis().getAnalyzerVersion() + ":"
                + record.getAnalysis().getAnalysisPolicyVersion()).collect(Collectors.toSet());
        TrainingRecord last = records.getLast();
        LocalDateTime measuredAt = metric == EvaluationPolicy.Metric.SUBJECTIVE_ANXIETY
                ? last.getAnxietyRecordedAt() != null ? last.getAnxietyRecordedAt() : last.getScoreAppliedAt() : last.getEndedAt();
        return new Point(value, measuredAt, records.stream().map(TrainingRecord::getRecordId).toList(), versions);
    }

    private MetricResult compared(EvaluationPolicy policy, EvaluationPolicy.Metric metric, Point before, Point after,
            int beforeCount, int afterCount) {
        BigDecimal change = before.value().subtract(after.value()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal improvement = metric.higherIsBetter() ? change.negate() : change;
        String direction = improvement.signum() > 0 ? "IMPROVED" : improvement.signum() < 0 ? "WORSENED" : "UNCHANGED";
        BigDecimal threshold = policy.thresholds().get(metric);
        Boolean meets = threshold == null ? null : improvement.signum() > 0 && improvement.compareTo(threshold) >= 0;
        List<Long> ids = new ArrayList<>(before.ids()); ids.addAll(after.ids());
        return new MetricResult("EVALUABLE", null, before.value(), after.value(), change, direction, meets,
                beforeCount, afterCount, List.copyOf(ids), before.versions(), before.at(), after.at());
    }

    public EvaluationResult.Summary summarize(List<EvaluationResult> results, Set<EvaluationPolicy.Metric> metrics) {
        Map<String, Long> sources = new TreeMap<>();
        results.forEach(result -> result.sources().forEach(source -> sources.merge(source, 1L, Long::sum)));
        Map<EvaluationPolicy.Metric, EvaluationResult.MetricSummary> summaries = new EnumMap<>(EvaluationPolicy.Metric.class);
        for (var metric : metrics) {
            List<MetricResult> available = results.stream().map(result -> result.metrics().get(metric))
                    .filter(result -> "EVALUABLE".equals(result.status())).toList();
            Map<String, Long> unavailable = results.stream().map(result -> result.metrics().get(metric))
                    .filter(result -> !"EVALUABLE".equals(result.status()))
                    .collect(Collectors.groupingBy(MetricResult::reason, TreeMap::new, Collectors.counting()));
            Map<String, List<MetricResult>> groups = new TreeMap<>();
            results.forEach(result -> {
                MetricResult value = result.metrics().get(metric);
                if ("EVALUABLE".equals(value.status())) {
                    String key = result.comparisonWindow() + ":" + String.join("|", new TreeSet<>(value.versions()));
                    groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
                }
            });
            Map<String, EvaluationResult.MetricSummary> strata = new TreeMap<>();
            if (groups.size() > 1) { groups.forEach((key, values) -> strata.put(key, summarizeMetric(values, Map.of(), Map.of()))); }
            summaries.put(metric, summarizeMetric(available, unavailable, strata));
        }
        return new EvaluationResult.Summary(results.size(), results.stream().filter(result -> result.trainingSessions() > 0).count(),
                results.stream().filter(result -> result.trainingDays() >= 10).count(),
                results.stream().map(result -> result.metrics().get(EvaluationPolicy.Metric.SELF_ASSESSMENT))
                        .filter(Objects::nonNull).filter(result -> result.beforeCount() > 0).count(),
                results.stream().map(result -> result.metrics().get(EvaluationPolicy.Metric.SELF_ASSESSMENT))
                        .filter(Objects::nonNull).filter(result -> result.afterCount() > 0).count(), sources, summaries);
    }

    private EvaluationResult.MetricSummary summarizeMetric(List<MetricResult> available, Map<String, Long> unavailable,
            Map<String, EvaluationResult.MetricSummary> strata) {
        boolean comparable = strata.isEmpty();
        long improved = available.stream().filter(result -> "IMPROVED".equals(result.direction())).count();
        List<BigDecimal> changes = available.stream().map(MetricResult::change).sorted().toList();
        BigDecimal median = changes.isEmpty() ? null : changes.size() % 2 == 1 ? changes.get(changes.size() / 2)
                : changes.get(changes.size() / 2 - 1).add(changes.get(changes.size() / 2)).divide(BigDecimal.TWO, 4, RoundingMode.HALF_UP);
        BigDecimal[] interval = comparable ? bootstrapMeanInterval(changes) : new BigDecimal[] {null, null};
        return new EvaluationResult.MetricSummary(available.size(),
                available.stream().filter(result -> result.after().compareTo(result.before()) < 0).count(),
                available.stream().filter(result -> result.after().compareTo(result.before()) == 0).count(),
                available.stream().filter(result -> result.after().compareTo(result.before()) > 0).count(), improved,
                available.stream().noneMatch(result -> result.meetsThreshold() != null) ? null
                        : available.stream().filter(result -> Boolean.TRUE.equals(result.meetsThreshold())).count(),
                available.isEmpty() || !comparable ? null : BigDecimal.valueOf(improved * 100)
                        .divide(BigDecimal.valueOf(available.size()), 2, RoundingMode.HALF_UP),
                comparable ? mean(available.stream().map(MetricResult::before).toList()) : null,
                comparable ? mean(available.stream().map(MetricResult::after).toList()) : null,
                comparable ? mean(changes) : null, comparable ? median : null, interval[0], interval[1], comparable, unavailable, strata);
    }

    private BigDecimal[] bootstrapMeanInterval(List<BigDecimal> values) {
        if (values.size() < 2) { return new BigDecimal[] {null, null}; }
        Random random = new Random(20261010L);
        double[] means = new double[2000];
        for (int sample = 0; sample < means.length; sample++) {
            double sum = 0;
            for (int index = 0; index < values.size(); index++) { sum += values.get(random.nextInt(values.size())).doubleValue(); }
            means[sample] = sum / values.size();
        }
        Arrays.sort(means);
        return new BigDecimal[] {BigDecimal.valueOf(means[49]).setScale(4, RoundingMode.HALF_UP),
                BigDecimal.valueOf(means[1949]).setScale(4, RoundingMode.HALF_UP)};
    }

    private static BigDecimal mean(List<BigDecimal> values) {
        return values.isEmpty() ? null : values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }
}
