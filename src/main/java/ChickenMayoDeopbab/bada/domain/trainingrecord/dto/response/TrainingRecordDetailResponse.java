package ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response;

import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.session.model.TranscriptTurn;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;

import java.time.LocalDateTime;
import java.util.List;

public record TrainingRecordDetailResponse(
        Long recordId,
        String sessionId,
        LocalDateTime trainedAt,
        Long scenarioId,
        String scenarioName,
        SessionType sessionType,
        AiPersonality aiPersonality,
        Long durationSeconds,
        String recordingUrl,
        List<TranscriptTurn> transcript,
        List<PositiveFeedbackResponse> positiveFeedbacks
) {
    public static TrainingRecordDetailResponse of(
            TrainingRecord record,
            List<TranscriptTurn> transcript,
            List<PositiveFeedbackResponse> positiveFeedbacks
    ) {
        return new TrainingRecordDetailResponse(
                record.getRecordId(),
                record.getSessionId(),
                record.getStartedAt(),
                record.getScenarioId(),
                record.getScenarioName(),
                record.getSessionType(),
                record.getAiPersonality(),
                record.getDurationSeconds(),
                record.getRecordingUrl(),
                transcript,
                positiveFeedbacks
        );
    }
}
