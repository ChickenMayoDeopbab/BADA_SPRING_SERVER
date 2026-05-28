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
        String email,
        @NotBlank
        String name
) {
        public Users toEntity(String password) {
                return Users.builder()
                        .username(username)
                        .password(password)
                        .email(email)
                        .name(name)
                        .build();
        }
}
