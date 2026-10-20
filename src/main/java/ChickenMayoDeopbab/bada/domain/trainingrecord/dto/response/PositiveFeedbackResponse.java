package ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response;

import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public record PositiveFeedbackResponse(
        Double startSecond,
        Double endSecond,
        String title,
        String content,
        // content 를 읽기 전에 배포된 앱이 읽는 사본
        @JsonProperty("good_point") String goodPoint,
        String summary,
        String audioUrl
) {
    public static PositiveFeedbackResponse from(GoodSegment segment, String audioUrl) {
        return new PositiveFeedbackResponse(
                segment.start(),
                segment.end(),
                segment.title(),
                segment.content(),
                Objects.requireNonNullElse(segment.content(), ""),
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
