package ChickenMayoDeopbab.bada.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CheckUsernameRequest(
        @NotBlank
        String username
) {
}
