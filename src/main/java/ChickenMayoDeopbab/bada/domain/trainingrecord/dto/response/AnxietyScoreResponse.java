package ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response;

import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;

public record AnxietyScoreResponse(
        Long recordId,
        String sessionId,
        Short anxietyScore
) {
    public static AnxietyScoreResponse from(TrainingRecord record) {
        return new AnxietyScoreResponse(
                record.getRecordId(),
                record.getSessionId(),
                record.getAnxietyScore()
        );
    }
}
