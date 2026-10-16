package ChickenMayoDeopbab.bada.domain.session.dto.request;

import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.AnalysisQualityStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionClosedRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsNoAudioReasonFromFastApiCallback() throws Exception {
        String json = """
                {
                  "reason": "NO_AUDIO",
                  "transcript": [],
                  "silence_total": 12.5,
                  "shake_count": 0,
                  "good_segments": [],
                  "recording_key": null
                }
                """;

        SessionClosedRequest request = objectMapper.readValue(json, SessionClosedRequest.class);

        assertThat(request.reason()).isEqualTo(EndReason.NO_AUDIO);
        assertThat(request.speechMetrics()).isNull();
    }

    @Test
    void preservesSnakeCaseSpeechSummaryThroughStorageSerialization() throws Exception {
        String json = """
                {"reason":"END_CALL", "speech_metrics":{
                  "schema_version":1, "measurement_version":"FEEDBACK_METRICS_V1", "policy_version":"FEEDBACK_POLICY_V1",
                  "metrics":{"tremor_turn_rate":{"status":"MEASURED","eligible_turn_count":5,"affected_turn_count":0},
                             "response_delay_ms":{"status":"UNAVAILABLE","eligible_turn_count":0}}
                }}
                """;
        var request = objectMapper.readValue(json, SessionClosedRequest.class);
        var summary = request.speechMetrics();
        assertThat(summary.supportsCurrentPolicy()).isTrue();
        assertThat(summary.metrics().get("tremor_turn_rate").hasValidRate()).isTrue();
        assertThat(summary.metrics().get("response_delay_ms").hasValidDelay()).isFalse();
        var stored = objectMapper.readTree(objectMapper.writeValueAsString(summary));
        assertThat(stored.get("schema_version").intValue()).isEqualTo(1);
        assertThat(stored.get("metrics").get("tremor_turn_rate").get("affected_turn_count").intValue()).isZero();
    }

    @Test
    void readsTrainingAnalysisFromFastApiCallback() throws Exception {
        String json = """
                {
                  "reason": "SCENARIO_DONE",
                  "transcript": [],
                  "silence_total": 3.5,
                  "shake_count": 1,
                  "good_segments": [],
                  "recording_key": null,
                  "analysis": {
                    "stability_score": 82.5,
                    "conversation_score": 75.0,
                    "fluency_score": 80.0,
                    "user_speech_duration_ms": 18200,
                    "ai_speech_duration_ms": 24100,
                    "server_wait_duration_ms": 3200,
                    "valid_user_turn_count": 4,
                    "user_tremor_duration_ms": 900,
                    "user_sustained_speech_duration_ms": 5100,
                    "completed_script_steps": 3,
                    "script_step_count": 4,
                    "analysis_quality_status": "PASS",
                    "analysis_exclusion_reason": null,
                    "analyzer_version": "VOICE_METRICS_V1",
                    "analysis_policy_version": "ANALYSIS_POLICY_V1"
                  }
                }
                """;

        SessionClosedRequest request =
                objectMapper.readValue(json, SessionClosedRequest.class);

        assertThat(request.analysis()).isNotNull();

        assertThat(request.analysis().stabilityScore())
                .isEqualByComparingTo("82.5");

        assertThat(request.analysis().analysisQualityStatus())
                .isEqualTo(AnalysisQualityStatus.PASS);
    }
}
