package ChickenMayoDeopbab.bada.domain.auth.dto.request;

import ChickenMayoDeopbab.bada.domain.auth.enums.AuthEmailType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmailSendRequest(
        @NotBlank
        @Email
        String email,
        @NotNull
        AuthEmailType type
) {
}
