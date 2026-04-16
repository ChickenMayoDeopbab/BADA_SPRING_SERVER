package ChickenMayoDeopbab.bada.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RefreshRequest(
        @NotBlank
        String refreshToken,
        @NotNull
        Long userId
) {
}
