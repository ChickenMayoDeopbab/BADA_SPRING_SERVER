package ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response;

import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public record GoodSegmentResponse(
        Double start,
        Double end,
        String title,
        String content,
        // content 를 읽기 전에 배포된 앱이 읽는 사본
        @JsonProperty("good_point") String goodPoint
) {
    public static GoodSegmentResponse from(GoodSegment segment) {
        return new GoodSegmentResponse(
                segment.start(),
                segment.end(),
                segment.title(),
                segment.content(),
                Objects.requireNonNullElse(segment.content(), "")
        );
    }
}
