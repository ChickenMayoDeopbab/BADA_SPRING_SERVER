package ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response;

import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;

import java.time.LocalDateTime;

public record TrainingRecordResponse(
        Long recordId,
        String sessionId,
        LocalDateTime trainedAt,
        String scenarioName,
        SessionType sessionType,
        Long durationSeconds,
        String categoryIconUrl
) {
    public static TrainingRecordResponse from(TrainingRecord record, String categoryIconUrl) {
        return new TrainingRecordResponse(
                record.getRecordId(),
                record.getSessionId(),
                record.getStartedAt(),
                record.getScenarioName(),
                record.getSessionType(),
                record.getDurationSeconds(),
                categoryIconUrl
        );
    }
}
