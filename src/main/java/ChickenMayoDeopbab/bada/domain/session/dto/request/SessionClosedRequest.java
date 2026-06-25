package ChickenMayoDeopbab.bada.domain.session.dto.request;

import ChickenMayoDeopbab.bada.domain.session.dto.response.GoodSegments;
import ChickenMayoDeopbab.bada.domain.session.dto.response.SessionClosedResponse;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.model.TranscriptTurn;

import java.util.List;

// FastAPI에서 Spring으로 세션 종료 콜백
public record SessionClosedRequest(
        EndReason reason,
        List<TranscriptTurn> transcript,
        float silence_total,
        int shake_count,
        List<GoodSegments> good_segments
) {
}
