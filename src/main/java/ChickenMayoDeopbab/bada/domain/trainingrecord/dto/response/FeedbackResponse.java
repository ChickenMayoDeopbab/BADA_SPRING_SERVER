package ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response;

import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;

import java.util.List;

public record FeedbackResponse(
        List<GoodSegment> goodSegments,
        String recordingUrl
) {
    public static FeedbackResponse of(TrainingRecord trainingRecord, List<GoodSegment> goodSegments) {
        return new FeedbackResponse(
                goodSegments,
                trainingRecord.getRecordingUrl()
        );
    }
}
