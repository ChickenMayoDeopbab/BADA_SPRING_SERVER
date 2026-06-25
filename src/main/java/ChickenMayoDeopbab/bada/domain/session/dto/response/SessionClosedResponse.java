package ChickenMayoDeopbab.bada.domain.session.dto.response;

import ChickenMayoDeopbab.bada.domain.session.dto.request.SessionClosedRequest;

import java.util.List;

public record SessionClosedResponse(
        float silence_total,
        int shake_count,
        List<GoodSegments> good_segments
) {
    public static SessionClosedResponse of(SessionClosedRequest request) {
        return new SessionClosedResponse(
                request.silence_total(),
                request.shake_count(),
                request.good_segments()
        );
    }
}
