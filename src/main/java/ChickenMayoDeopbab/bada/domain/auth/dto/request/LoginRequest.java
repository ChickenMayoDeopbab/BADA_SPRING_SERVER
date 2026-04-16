package ChickenMayoDeopbab.bada.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "ID는 필수 입력입니다.")
        String username,
        @NotBlank(message = "비밀번호는 필수 입력입니다.")
        String password
) {
}
