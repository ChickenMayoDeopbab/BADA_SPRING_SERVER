package ChickenMayoDeopbab.bada.domain.session.dto.response;

import ChickenMayoDeopbab.bada.domain.session.dto.request.SessionClosedRequest;
import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;

import java.util.List;

public record SessionClosedResponse(
        Double silence_total,
        Integer shake_count,
        List<GoodSegment> good_segments
) {
    public static SessionClosedResponse of(SessionClosedRequest request) {
        return new SessionClosedResponse(
                request.silenceTotal(),
                request.shakeCount(),
                request.goodSegments()
        );
    }
}
