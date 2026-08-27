package ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response;

import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;
import ChickenMayoDeopbab.bada.domain.session.model.TranscriptTurn;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

public record FeedbackResponse(
        SessionType sessionType,
        String scenarioName,
        @JsonFormat(pattern = "HH:mm:ss")
        LocalTime trainingTime,
        List<GoodSegment> goodSegments,
        String recordingUrl,
        List<TranscriptTurn> transcript
) {
    public static FeedbackResponse of(TrainingRecord trainingRecord, List<GoodSegment> goodSegments, String recordingUrl, List<TranscriptTurn> transcript) {
        return new FeedbackResponse(
                trainingRecord.getSessionType(),
                trainingRecord.getScenarioName(),
                LocalTime.ofSecondOfDay
                        (Duration.between(trainingRecord.getStartedAt(), trainingRecord.getEndedAt()).getSeconds()),
                goodSegments,
                recordingUrl,
                transcript
        );
    }
}
