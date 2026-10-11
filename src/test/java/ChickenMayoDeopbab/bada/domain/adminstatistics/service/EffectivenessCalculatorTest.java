package ChickenMayoDeopbab.bada.domain.adminstatistics.service;

import ChickenMayoDeopbab.bada.domain.adminstatistics.model.*;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.*;
import ChickenMayoDeopbab.bada.domain.session.enums.*;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request.TrainingAnalysisRequest;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.*;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

import static ChickenMayoDeopbab.bada.domain.adminstatistics.model.EvaluationPolicy.Metric.*;
import static org.assertj.core.api.Assertions.*;

class EffectivenessCalculatorTest {
    private final EffectivenessCalculator calculator = new EffectivenessCalculator();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final LocalDateTime start = LocalDateTime.of(2026, 10, 1, 12, 0);
    private final LocalDateTime asOf = start.plusDays(40);
    private final Users user = Users.builder().userId(1L).build();
    private final EffectivenessCalculator.Participant participant =
            new EffectivenessCalculator.Participant(1L, Set.of("BETA"), start, null, null, null);

    @Test
    void constantMeasurementsDoNotBecomeImprovementThroughRepeatedTraining() {
        var policy = policy("metrics", Set.of(SELF_ASSESSMENT, SUBJECTIVE_ANXIETY));
        var result = calculator.evaluate(policy, participant, asOf,
                List.of(diagnosis(1, start, 4, "V1"), diagnosis(2, start.plusDays(14), 4, "V1")), records(6));
        assertThat(result.metrics().get(SELF_ASSESSMENT).direction()).isEqualTo("UNCHANGED");
        assertThat(result.metrics().get(SUBJECTIVE_ANXIETY).direction()).isEqualTo("UNCHANGED");
        assertThat(calculator.summarize(List.of(result), policy.metrics()).metrics().get(SELF_ASSESSMENT).improvementRate())
                .isEqualByComparingTo("0");
    }

    @Test
    void surveyComparisonDoesNotRequireAnySuccessfulAudioAnalysis() {
        var policy = policy("metrics", Set.of(SELF_ASSESSMENT));
        var result = calculator.evaluate(policy, participant, asOf,
                List.of(diagnosis(1, start, 4, "V1"), diagnosis(2, start.plusDays(14), 3, "V1")), List.of());
        assertThat(result.metrics().get(SELF_ASSESSMENT).change()).isEqualByComparingTo("1");
        assertThat(result.trainingSessions()).isZero();
    }

    @Test
    void missingPostRemainsInSelectedPopulationWithoutCountingAsUnchanged() {
        var policy = policy("metrics", Set.of(SELF_ASSESSMENT));
        var result = calculator.evaluate(policy, participant, asOf, List.of(diagnosis(1, start, 4, "V1")), List.of());
        var summary = calculator.summarize(List.of(result), policy.metrics());
        assertThat(summary.selectedUsers()).isEqualTo(1);
        assertThat(summary.baselineRecordedUsers()).isEqualTo(1);
        assertThat(summary.postRecordedUsers()).isZero();
        assertThat(summary.metrics().get(SELF_ASSESSMENT).evaluableUsers()).isZero();
        assertThat(summary.metrics().get(SELF_ASSESSMENT).improvementRate()).isNull();
        assertThat(summary.metrics().get(SELF_ASSESSMENT).unavailableReasons()).containsEntry("MISSING_POST_ASSESSMENT", 1L);
    }

    @Test
    void baselineCountIsRetainedBeforeEvaluationOrTrainingMilestoneIsDue() {
        for (var policy : List.of(policy("metrics", Set.of(SELF_ASSESSMENT)),
                policy("metrics", Set.of(SELF_ASSESSMENT), "windowMode", "TRAINING_DAYS", "targetCount", 10))) {
            var result = calculator.evaluate(policy, participant, start.plusDays(1),
                    List.of(diagnosis(1, start, 4, "V1")), List.of());
            var summary = calculator.summarize(List.of(result), policy.metrics());
            assertThat(summary.baselineRecordedUsers()).isEqualTo(1);
            assertThat(summary.postRecordedUsers()).isZero();
            assertThat(summary.metrics().get(SELF_ASSESSMENT).evaluableUsers()).isZero();
            assertThat(result.metrics().get(SELF_ASSESSMENT).reason()).isEqualTo("NOT_DUE");
        }
    }

    @Test
    void repeatedCallsOnOneDayDoNotCompleteTenTrainingDays() {
        var policy = policy("windowMode", "TRAINING_DAYS", "targetCount", 10);
        List<TrainingRecord> records = IntStream.range(0, 10).mapToObj(index -> record(index + 1, start.plusMinutes(index + 1), "V3")).toList();
        var result = calculator.evaluate(policy, participant, start.plusDays(10), List.of(), records);
        assertThat(result.trainingDays()).isEqualTo(1);
        assertThat(result.end()).isNull();
        assertThat(result.metrics().get(SELF_ASSESSMENT).reason()).isEqualTo("NOT_DUE");
    }

    @Test
    void nonConsecutiveTenDaysCompleteMilestoneAndSupportLateIosParticipation() {
        var policy = policy("windowMode", "TRAINING_DAYS", "targetCount", 10);
        List<TrainingRecord> records = IntStream.range(0, 10).mapToObj(index -> record(index + 1, start.plusDays(index * 2L).plusMinutes(1), "V3")).toList();
        var result = calculator.evaluate(policy, participant, asOf, List.of(), records);
        assertThat(result.trainingDays()).isEqualTo(10);
        assertThat(result.end()).isEqualTo(start.plusDays(18).plusMinutes(1));
    }

    @Test
    void trainingAfterMaximumObservationPeriodDoesNotCompleteMilestoneOrEnterDenominator() {
        var policy = policy("windowMode", "TRAINING_DAYS", "targetCount", 10);
        List<TrainingRecord> training = IntStream.range(0, 10)
                .mapToObj(index -> record(index + 1, start.plusDays(index * 11L).plusMinutes(1), "V3")).toList();
        var result = calculator.evaluate(policy, participant, start.plusDays(120), List.of(), training);
        assertThat(result.end()).isNull();
        assertThat(result.trainingDays()).isEqualTo(9);
        assertThat(result.trainingRecordIds()).doesNotContain(10L);
        assertThat(result.metrics().get(SELF_ASSESSMENT).reason()).isEqualTo("MILESTONE_NOT_REACHED");
        assertThat(calculator.summarize(List.of(result), policy.metrics()).tenDayCompleters()).isZero();
    }

    @Test
    void elapsedObservationClosesEvenWhenTenDayMilestoneWasNotReached() {
        var policy = policy("windowMode", "ELAPSED_DAYS", "targetCount", 14);
        var result = calculator.evaluate(policy, participant, asOf, List.of(), records(2));
        assertThat(result.end()).isEqualTo(start.plusDays(14));
        assertThat(result.metrics().get(SELF_ASSESSMENT).reason()).isEqualTo("MISSING_BASELINE");
    }

    @Test
    void questionnaireVersionsMustMatchAndUnknownVersionsRequireExplicitOptIn() {
        var policy = policy("metrics", Set.of(SELF_ASSESSMENT));
        var mismatch = calculator.evaluate(policy, participant, asOf,
                List.of(diagnosis(1, start, 4, "V1"), diagnosis(2, start.plusDays(14), 3, "V2")), List.of());
        assertThat(mismatch.metrics().get(SELF_ASSESSMENT).reason()).isEqualTo("QUESTIONNAIRE_VERSION_MISMATCH");
        var legacy = List.of(diagnosis(1, start, 4, null), diagnosis(2, start.plusDays(14), 3, null));
        assertThat(calculator.evaluate(policy, participant, asOf, legacy, List.of()).metrics().get(SELF_ASSESSMENT).reason())
                .isEqualTo("UNKNOWN_QUESTIONNAIRE_VERSION");
        assertThat(calculator.evaluate(policy("allowLegacyAssessments", true), participant, asOf, legacy, List.of())
                .metrics().get(SELF_ASSESSMENT).versions()).containsExactly("LEGACY_UNVERIFIED:UNKNOWN");
    }

    @Test
    void nearestPostIsChosenInsteadOfLifetimeLatest() {
        var result = calculator.evaluate(policy(), participant, asOf, List.of(diagnosis(1, start, 4, "V1"),
                diagnosis(2, start.plusDays(14), 3, "V1"), diagnosis(3, start.plusDays(30), 1, "V1")), List.of());
        assertThat(result.metrics().get(SELF_ASSESSMENT).sourceIds()).containsExactly(1L, 2L);
    }

    @Test
    void earlyAndLateSamplesCannotOverlap() {
        var result = calculator.evaluate(policy(), participant, asOf, List.of(), records(5));
        assertThat(result.metrics().get(SUBJECTIVE_ANXIETY).reason()).isEqualTo("INSUFFICIENT_NON_OVERLAPPING_OBSERVATIONS");
    }

    @Test
    void analysisVersionsCannotBeMixedEvenWhenScoresArePresent() {
        List<TrainingRecord> records = records(6);
        ReflectionTestUtils.setField(records.getLast().getAnalysis(), "analyzerVersion", "V2");
        var result = calculator.evaluate(policy("metrics", Set.of(STABILITY)), participant, asOf, List.of(), records);
        assertThat(result.metrics().get(STABILITY).reason()).isEqualTo("ANALYZER_VERSION_MISMATCH");
    }

    @Test
    void difficultyMismatchIsExplicitAndCanBeAllowedForExploratoryAnalysis() {
        List<TrainingRecord> records = records(6);
        ReflectionTestUtils.setField(records.getLast(), "difficulty", "EASY");
        assertThat(calculator.evaluate(policy(), participant, asOf, List.of(), records).metrics().get(SUBJECTIVE_ANXIETY).reason())
                .isEqualTo("TRAINING_CONDITION_MISMATCH");
        assertThat(calculator.evaluate(policy("matchTrainingConditions", false), participant, asOf, List.of(), records)
                .metrics().get(SUBJECTIVE_ANXIETY).status()).isEqualTo("EVALUABLE");
    }

    @Test
    void differentSurveyVersionsAcrossPeopleAreReportedInSeparateStrata() {
        var policy = policy("metrics", Set.of(SELF_ASSESSMENT));
        var first = calculator.evaluate(policy, participant, asOf,
                List.of(diagnosis(1, start, 4, "V1"), diagnosis(2, start.plusDays(14), 3, "V1")), List.of());
        var second = calculator.evaluate(policy, participant, asOf,
                List.of(diagnosis(3, start, 4, "V2"), diagnosis(4, start.plusDays(14), 2, "V2")), List.of());
        var summary = calculator.summarize(List.of(first, second), policy.metrics()).metrics().get(SELF_ASSESSMENT);
        assertThat(summary.comparable()).isFalse();
        assertThat(summary.meanChange()).isNull();
        assertThat(summary.strata()).hasSize(2);
    }

    @Test
    void onePersonHasNoConfidenceIntervalAndSameScaleHasPairedMean() {
        var policy = policy("metrics", Set.of(SELF_ASSESSMENT));
        var first = calculator.evaluate(policy, participant, asOf,
                List.of(diagnosis(1, start, 4, "V1"), diagnosis(2, start.plusDays(14), 3, "V1")), List.of());
        var single = calculator.summarize(List.of(first), policy.metrics()).metrics().get(SELF_ASSESSMENT);
        assertThat(single.ci95Lower()).isNull();
        var second = calculator.evaluate(policy, participant, asOf,
                List.of(diagnosis(3, start, 4, "V1"), diagnosis(4, start.plusDays(14), 2, "V1")), List.of());
        var summary = calculator.summarize(List.of(first, second), policy.metrics()).metrics().get(SELF_ASSESSMENT);
        assertThat(summary.meanChange()).isEqualByComparingTo("1.5");
        assertThat(summary.ci95Lower()).isEqualByComparingTo("1");
        assertThat(summary.ci95Upper()).isEqualByComparingTo("2");
    }

    @Test
    void futureOrUnavailableSubjectiveInputsAreNotImputed() {
        List<TrainingRecord> records = records(6);
        ReflectionTestUtils.setField(records.getLast(), "anxietyRecordedAt", asOf.plusDays(1));
        var result = calculator.evaluate(policy(), participant, asOf, List.of(), records);
        assertThat(result.metrics().get(SUBJECTIVE_ANXIETY).reason()).isEqualTo("INSUFFICIENT_NON_OVERLAPPING_OBSERVATIONS");
    }

    @Test
    void delayedAnxietyResponsesRequireExplicitExploratoryOptIn() {
        List<TrainingRecord> records = records(6);
        ReflectionTestUtils.setField(records.getLast(), "anxietyRecordedAt", records.getLast().getEndedAt().plusDays(2));
        assertThat(calculator.evaluate(policy(), participant, asOf, List.of(), records).metrics().get(SUBJECTIVE_ANXIETY).status())
                .isEqualTo("UNAVAILABLE");
        assertThat(calculator.evaluate(policy("maximumSubjectiveDelayMinutes", 0), participant, asOf, List.of(), records)
                .metrics().get(SUBJECTIVE_ANXIETY).status()).isEqualTo("EVALUABLE");
    }

    @Test
    void malformedPolicyIsRejected() {
        assertThatThrownBy(() -> policy("earlyCount", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy("windowMode", "FIXED_PERIOD")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy("thresholds", Map.of("SELF_ASSESSMENT", 5))).isInstanceOf(IllegalArgumentException.class);
    }

    private EvaluationPolicy policy(Object... pairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) { values.put((String) pairs[index], pairs[index + 1]); }
        return mapper.convertValue(values, EvaluationPolicy.class);
    }
    private DiagnosisResult diagnosis(long id, LocalDateTime at, double score, String version) {
        return DiagnosisResult.builder().questionId(id).user(user).sessionId("diagnosis-" + id).type(DiagnosisType.SIGNUP)
                .score(score).questionnaireVersion(version).questionnaireVersionConfirmed(version != null).createdAt(at).updatedAt(at).build();
    }
    private List<TrainingRecord> records(int count) {
        return IntStream.range(0, count).mapToObj(index -> record(index + 1, start.plusDays(index).plusMinutes(1), "V3")).toList();
    }
    private TrainingRecord record(long id, LocalDateTime at, String version) {
        var analysis = new TrainingAnalysisRequest(BigDecimal.valueOf(90), BigDecimal.valueOf(90), BigDecimal.valueOf(90),
                6000L, 1000L, 100L, 2, 0L, 4000L, 2, 2, AnalysisQualityStatus.PASS, null, version, "P3");
        TrainingRecord record = TrainingRecord.builder().user(user).sessionId("training-" + id).scenarioName("예약")
                .difficulty("HARD").scriptLevel(3).sessionType(SessionType.CUSTOM).endReason(EndReason.END_CALL)
                .startedAt(at.minusSeconds(30)).endedAt(at).analysis(TrainingAnalysisMetrics.from(analysis)).build();
        ReflectionTestUtils.setField(record, "recordId", id);
        record.recordAnxietyScore((short) 8);
        ReflectionTestUtils.setField(record, "anxietyRecordedAt", at.plusSeconds(1));
        return record;
    }
}
