package ChickenMayoDeopbab.bada.domain.trainingrecord.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

/** 통화 전체의 측정 분모를 보존하는 종료 시점 스냅샷. 구간 피드백 선택 결과와 독립적이다. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TrainingSpeechMetrics(
        Integer schemaVersion,
        String measurementVersion,
        String policyVersion,
        Map<String, Measurement> metrics
) {
    public boolean supportsCurrentPolicy() {
        return Integer.valueOf(1).equals(schemaVersion)
                && "FEEDBACK_METRICS_V1".equals(measurementVersion)
                && "FEEDBACK_POLICY_V1".equals(policyVersion);
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Measurement(
            String status,
            Integer eligibleTurnCount,
            Integer affectedTurnCount,
            Long totalDelayMs
    ) {
        public boolean hasMeasuredTurns() {
            return "MEASURED".equals(status) && eligibleTurnCount != null && eligibleTurnCount > 0;
        }

        public boolean hasValidRate() {
            return hasMeasuredTurns() && affectedTurnCount != null
                    && affectedTurnCount >= 0 && affectedTurnCount <= eligibleTurnCount;
        }

        public boolean hasValidDelay() {
            return hasMeasuredTurns() && totalDelayMs != null && totalDelayMs >= 0;
        }
    }
}
