package ChickenMayoDeopbab.bada.domain.user.dto.request;

import ChickenMayoDeopbab.bada.domain.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UserModerationStatusRequest(
        @NotNull UserStatus status,
        Instant suspendedUntil,
        @Size(max = 500) String reason,
        @NotNull Long actorUserId
) {
}
