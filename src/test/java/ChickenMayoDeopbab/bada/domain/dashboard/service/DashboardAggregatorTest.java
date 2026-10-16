package ChickenMayoDeopbab.bada.domain.dashboard.service;

import ChickenMayoDeopbab.bada.domain.dashboard.dto.request.DashboardQuery.DateRange;
import ChickenMayoDeopbab.bada.domain.dashboard.dto.request.DashboardQuery.Interval;
import ChickenMayoDeopbab.bada.domain.dashboard.dto.response.DashboardAnalyticsResponse.*;
import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingSpeechMetrics;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.projection.TrainingDashboardRow;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardAggregatorTest {
    private final DashboardAggregator aggregator = new DashboardAggregator();
    private long id;

    @Test
    void compares_conditions_with_identical_weights_despite_distribution_changes() {
        List<TrainingDashboardRow> before = new ArrayList<>(), after = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            before.add(row(1L, "EASY", 2, null));
            after.add(row(2L, "HARD", 8, null));
        }
        for (int i = 0; i < 3; i++) {
            before.add(row(2L, "HARD", 8, null));
            after.add(row(1L, "EASY", 2, null));
        }
        MetricComparison result = aggregator.compare(before, after).metrics().getFirst();
        assertThat(result.overall().delta()).isEqualByComparingTo("3.23");
        assertThat(result.matched().delta()).isEqualByComparingTo("0");
        assertThat(result.matched().baseline().value()).isEqualByComparingTo("5");
        assertThat(result.matched().current().value()).isEqualByComparingTo("5");
        assertThat(result.matched().conditions()).extracting(ConditionComparison::weight).containsExactly(3L, 3L);
        assertThat(result.matched().baseline().sampleCount()).isEqualTo(13);
    }

    @Test
    void does_not_compare_different_or_unknown_conditions() {
        var before = List.of(row(1L, "EASY", 5, null), row(1L, "EASY", 5, null), row(1L, "EASY", 5, null));
        var after = List.of(row(1L, "HARD", 3, null), row(1L, "HARD", 3, null), row(1L, null, 3, null));
        var matched = aggregator.compare(before, after).metrics().getFirst().matched();
        assertThat(matched.status()).isEqualTo(Status.NOT_COMPARABLE);
        assertThat(matched.delta()).isNull();
        assertThat(matched.current().exclusionReasons()).containsEntry("UNKNOWN_CONDITION", 1L)
                .containsEntry("NO_SHARED_CONDITION", 2L);
    }

    @Test
    void requires_three_valid_samples_in_each_condition_and_period() {
        var before = List.of(row(1L, "EASY", 5, null), row(1L, "EASY", 5, null));
        var after = List.of(row(1L, "EASY", 3, null), row(1L, "EASY", 3, null), row(1L, "EASY", 3, null));
        var matched = aggregator.compare(before, after).metrics().getFirst().matched();
        assertThat(matched.status()).isEqualTo(Status.INSUFFICIENT_DATA);
        assertThat(matched.delta()).isNull();
        assertThat(matched.baseline().exclusionReasons()).containsEntry("LOW_SAMPLE_SIZE", 2L);
    }

    @Test
    void keeps_real_zero_separate_from_missing_and_preserves_other_measured_metrics() {
        var summary = speech(Map.of("tremor_turn_rate", new TrainingSpeechMetrics.Measurement("MEASURED", 3, 0, null)));
        var values = aggregator.metrics(List.of(row(1L, "EASY", 0, summary), row(1L, "EASY", null, null)));
        assertThat(values.get("tremorTurnRate").value()).isEqualByComparingTo("0");
        assertThat(values.get("tremorTurnRate").sampleCount()).isEqualTo(1);
        assertThat(values.get("tremorTurnRate").exclusionReasons()).containsEntry("MISSING_SPEECH_METRICS", 1L);
        assertThat(values.get("longPauseTurnRate").value()).isNull();
        assertThat(values.get("selfReportedAnxiety").value()).isEqualByComparingTo("0");
    }

    @Test
    void averages_session_means_instead_of_pooling_turns() {
        var first = speech(Map.of("response_delay_ms", new TrainingSpeechMetrics.Measurement("MEASURED", 10, null, 10000L)));
        var second = speech(Map.of("response_delay_ms", new TrainingSpeechMetrics.Measurement("MEASURED", 1, null, 3000L)));
        var result = aggregator.metrics(List.of(row(1L, "EASY", null, first), row(1L, "EASY", null, second))).get("responseDelayMs");
        assertThat(result.value()).isEqualByComparingTo("2000");
        assertThat(result.eligibleTurnCount()).isEqualTo(11);
    }

    @Test
    void excludes_unknown_versions_and_invalid_measurements_independently() {
        var future = new TrainingSpeechMetrics(2, "FEEDBACK_METRICS_V2", "FEEDBACK_POLICY_V2", Map.of());
        var invalid = speech(Map.of("tremor_turn_rate", new TrainingSpeechMetrics.Measurement("MEASURED", 2, 3, null)));
        var rows = List.of(row(1L, "EASY", 4, future), row(1L, "EASY", 3, invalid));
        var value = aggregator.metrics(rows).get("tremorTurnRate");
        assertThat(value.value()).isNull();
        assertThat(value.exclusionReasons()).containsEntry("UNSUPPORTED_METRICS_VERSION", 1L).containsEntry("INVALID_MEASUREMENT", 1L);
        assertThat(aggregator.metrics(rows).get("selfReportedAnxiety").sampleCount()).isEqualTo(2);
        assertThat(aggregator.compare(rows, rows).metrics().get(1).matched().status()).isEqualTo(Status.NO_DATA);
    }

    @Test
    void separates_activity_from_speech_quality_and_warmup() {
        var speech = speech(Map.of("tremor_turn_rate", new TrainingSpeechMetrics.Measurement("MEASURED", 3, 0, null)));
        var source = row(1L, "EASY", 3, speech);
        var userEnd = withTypeAndReason(source, SessionType.SCENARIO, EndReason.USER_END);
        var error = withTypeAndReason(source, SessionType.SCENARIO, EndReason.ERROR);
        var warmup = withTypeAndReason(source, SessionType.WARMUP, EndReason.END_CALL);
        var rows = List.of(userEnd, error, warmup);
        assertThat(aggregator.summary(rows).trainingCount()).isEqualTo(1);
        assertThat(aggregator.summary(rows).warmupCount()).isEqualTo(1);
        assertThat(aggregator.metrics(rows).get("selfReportedAnxiety").sampleCount()).isEqualTo(1);
        assertThat(aggregator.metrics(rows).get("tremorTurnRate").exclusionReasons()).containsEntry("UNSUPPORTED_END_REASON", 1L);
    }

    @Test
    void returns_empty_buckets_and_marks_clipped_weeks() {
        var range = new DateRange(LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 12));
        var points = aggregator.series(List.of(), range, Interval.WEEK, LocalDate.of(2026, 10, 17));
        assertThat(points).hasSize(3);
        assertThat(points).extracting(SeriesPoint::isPartial).containsExactly(true, false, true);
        assertThat(points.get(1).metrics().get("selfReportedAnxiety").value()).isNull();
        assertThat(aggregator.activity(List.of(), range)).hasSize(9).allMatch(point -> point.trainingCount() == 0);
    }

    private TrainingDashboardRow row(Long scenario, String difficulty, Integer anxiety, TrainingSpeechMetrics metrics) {
        return new TrainingDashboardRow(++id, scenario, "예약", "SCENARIO_V1", difficulty, 1, SessionType.SCENARIO,
                AiPersonality.KIND, EndReason.END_CALL, LocalDateTime.of(2026, 10, 10, 12, 0), 60L,
                anxiety == null ? null : anxiety.shortValue(), metrics);
    }

    private TrainingSpeechMetrics speech(Map<String, TrainingSpeechMetrics.Measurement> metrics) {
        return new TrainingSpeechMetrics(1, "FEEDBACK_METRICS_V1", "FEEDBACK_POLICY_V1", metrics);
    }

    private TrainingDashboardRow withTypeAndReason(TrainingDashboardRow row, SessionType type, EndReason reason) {
        return new TrainingDashboardRow(++id, row.scenarioId(), row.scenarioName(), row.scenarioVersion(), row.difficulty(),
                row.scriptLevel(), type, row.aiPersonality(), reason, row.startedAt(), row.durationSeconds(), row.anxietyScore(), row.speechMetrics());
    }
}
