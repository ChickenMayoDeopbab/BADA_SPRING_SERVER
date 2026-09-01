package ChickenMayoDeopbab.bada.domain.notification.dto.request;

import ChickenMayoDeopbab.bada.domain.notification.entity.PushPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterPushDeviceRequest(
        @NotBlank
        @Size(max = 100)
        String installationId,

        @NotBlank
        @Size(max = 512)
        String token,

        @NotNull
        PushPlatform platform
) {
}
