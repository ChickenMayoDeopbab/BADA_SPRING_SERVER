package ChickenMayoDeopbab.bada.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record OAuthTokenResponse(
        String accessToken,

        String refreshToken,

        // boolean의 is 접두사는 Jackson이 떼어 newUser로 내보낼 수 있어 이름을 고정한다.
        @Schema(description = "이번 소셜 로그인에서 계정이 새로 만들어졌는지 여부")
        @JsonProperty("isNewUser")
        boolean isNewUser
) {
}
