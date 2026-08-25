package ChickenMayoDeopbab.bada.domain.session.dto.request;

import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;
import ChickenMayoDeopbab.bada.domain.session.model.TranscriptTurn;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request.TrainingAnalysisRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// FastAPI에서 Spring으로 세션 종료 콜백
public record SessionClosedRequest(
        EndReason reason,
        List<TranscriptTurn> transcript,

        @JsonProperty("silence_total")
        Double silenceTotal,

        @JsonProperty("shake_count")
        Integer shakeCount,

        @JsonProperty("good_segments")
        List<GoodSegment> goodSegments,

        @JsonProperty("recording_key")
        String recordingKey,

        TrainingAnalysisRequest analysis
) {
}
