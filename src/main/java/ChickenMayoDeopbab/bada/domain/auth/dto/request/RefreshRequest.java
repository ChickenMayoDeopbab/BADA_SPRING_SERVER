package ChickenMayoDeopbab.bada.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank
        String refreshToken,
        @NotBlank
        String username
) {
}
