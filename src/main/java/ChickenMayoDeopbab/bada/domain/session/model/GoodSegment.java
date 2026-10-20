package ChickenMayoDeopbab.bada.domain.session.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoodSegment(
        Double start,
        Double end,
        String title,
        @JsonAlias("good_point") String content
) {
}
