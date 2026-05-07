package ChickenMayoDeopbab.bada.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmailVerificationRequest(
        @NotBlank
        @Email
        String email,
        @NotNull
        String authNum
) {
}
