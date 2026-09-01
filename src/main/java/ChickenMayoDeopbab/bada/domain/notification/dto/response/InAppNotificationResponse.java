package ChickenMayoDeopbab.bada.domain.notification.dto.response;

import ChickenMayoDeopbab.bada.domain.notification.entity.InAppNotification;
import ChickenMayoDeopbab.bada.domain.notification.entity.InAppNotificationType;

import java.time.LocalDateTime;

public record InAppNotificationResponse(
        Long notificationId,
        InAppNotificationType type,
        String title,
        String message,
        Long actorUserId,
        String actorName,
        String actorProfileImage,
        Long postId,
        Long commentId,
        Long scheduleId,
        boolean read,
        LocalDateTime createdAt
) {
    public static InAppNotificationResponse from(InAppNotification notification) {
        return new InAppNotificationResponse(
                notification.getNotificationId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getActorUserId(),
                notification.getActorName(),
                notification.getActorProfileImage(),
                notification.getPostId(),
                notification.getCommentId(),
                notification.getScheduleId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
