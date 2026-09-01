package ChickenMayoDeopbab.bada.domain.notification.dto.response;

import org.springframework.data.domain.Page;

public record InAppNotificationListResponse(
        Page<InAppNotificationResponse> notifications,
        long unreadCount
) {
}
