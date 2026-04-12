package ChickenMayoDeopbab.bada.domain.user.dto.request;

import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignUpRequest(
        @NotBlank
        String username,
        @NotBlank
        String password,
        @NotBlank
        @Email
        String email
) {
        public Users toEntity(String username, String password, String email) {
                return Users.builder()
                        .username(username)
                        .password(password)
                        .email(email)
                        .build();
        }
}
