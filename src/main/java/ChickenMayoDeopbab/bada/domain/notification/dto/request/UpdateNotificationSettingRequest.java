package ChickenMayoDeopbab.bada.domain.notification.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationSettingRequest(
        @NotNull
        Boolean allEnabled,

        @NotNull
        Boolean communityEnabled,

        @NotNull
        Boolean trainingEnabled
) {
}
