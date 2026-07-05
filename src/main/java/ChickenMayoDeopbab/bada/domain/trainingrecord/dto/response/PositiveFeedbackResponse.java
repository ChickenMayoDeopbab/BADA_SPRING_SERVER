package ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response;

import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;

public record PositiveFeedbackResponse(
        Double startSecond,
        Double endSecond,
        String good_point,
        String summary,
        String audioUrl
) {
    public static PositiveFeedbackResponse from(GoodSegment segment) {
        return new PositiveFeedbackResponse(
                segment.start(),
                segment.end(),
                segment.goodPoint(),
                null,
                null
        );
    }
}
