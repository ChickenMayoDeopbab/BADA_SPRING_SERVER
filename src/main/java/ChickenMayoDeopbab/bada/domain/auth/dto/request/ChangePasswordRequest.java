package ChickenMayoDeopbab.bada.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank
        @Email
        String email,
        @NotBlank
        String oldPassword,
        @NotBlank
        String newPassword

) {
}
