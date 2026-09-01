package ChickenMayoDeopbab.bada.domain.notification.dto.request;

import ChickenMayoDeopbab.bada.domain.notification.model.CommunityNotificationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CommunityNotificationRequest(
        @NotNull
        CommunityNotificationType type,

        @NotNull
        @Positive
        Long recipientUserId,

        @NotNull
        @Positive
        Long actorUserId,

        @NotNull
        @Positive
        Long postId,

        @NotNull
        @Positive
        Long commentId
) {
}
