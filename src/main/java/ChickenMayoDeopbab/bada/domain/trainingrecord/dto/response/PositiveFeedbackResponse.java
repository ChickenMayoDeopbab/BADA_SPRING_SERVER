package ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response;

import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;

public record PositiveFeedbackResponse(
        Double startSecond,
        Double endSecond,
        String good_point,
        String summary,
        String audioUrl
) {
    public static PositiveFeedbackResponse from(GoodSegment segment, String audioUrl) {
        return new PositiveFeedbackResponse(
                segment.start(),
                segment.end(),
                segment.goodPoint(),
                null,
                toSegmentUrl(audioUrl, segment)
        );
    }

    private static String toSegmentUrl(String audioUrl, GoodSegment segment) {
        if (audioUrl == null || segment.start() == null || segment.end() == null) {
            return audioUrl;
        }
        return audioUrl + "#t=" + segment.start() + "," + segment.end();
    }
}
