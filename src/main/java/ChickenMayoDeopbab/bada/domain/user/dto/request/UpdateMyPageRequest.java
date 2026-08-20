package ChickenMayoDeopbab.bada.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateMyPageRequest(
        @NotBlank
        String name,
        @NotBlank
        String username,
        String s3Key
) {
}
