package ChickenMayoDeopbab.bada.domain.session.model;

import com.fasterxml.jackson.annotation.JsonProperty;

// FastAPI가 전달하는 잘한 발화 구간(초 단위)
public record GoodSegment(Double start, Double end, @JsonProperty("good_point") String goodPoint) {
}
