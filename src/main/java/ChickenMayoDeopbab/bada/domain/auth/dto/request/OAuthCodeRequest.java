package ChickenMayoDeopbab.bada.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OAuthCodeRequest(
        @NotBlank
        String code
) {
}