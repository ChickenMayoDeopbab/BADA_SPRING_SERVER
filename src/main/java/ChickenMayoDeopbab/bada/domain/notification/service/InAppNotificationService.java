package ChickenMayoDeopbab.bada.domain.notification.service;

import ChickenMayoDeopbab.bada.domain.notification.dto.response.InAppNotificationListResponse;
import ChickenMayoDeopbab.bada.domain.notification.dto.response.InAppNotificationResponse;
import ChickenMayoDeopbab.bada.domain.notification.entity.InAppNotification;
import ChickenMayoDeopbab.bada.domain.notification.exception.NotificationStatusCode;
import ChickenMayoDeopbab.bada.domain.notification.model.NotificationFilter;
import ChickenMayoDeopbab.bada.domain.notification.repository.InAppNotificationRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InAppNotificationService {

    private static final int RETENTION_DAYS = 14;

    private final InAppNotificationRepository inAppNotificationRepository;
    private final UsersRepository usersRepository;

    @Transactional(readOnly = true)
    public InAppNotificationListResponse getNotifications(
            NotificationFilter filter,
            Pageable pageable
    ) {
        Users user = getUserInfo();
        LocalDateTime createdAfter = LocalDateTime.now().minusDays(RETENTION_DAYS);

        Page<InAppNotification> notifications = switch (filter) {
            case ALL -> inAppNotificationRepository
                    .findByRecipientAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                            user,
                            createdAfter,
                            pageable
                    );
            case UNREAD -> inAppNotificationRepository
                    .findByRecipientAndReadAtIsNullAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                            user,
                            createdAfter,
                            pageable
                    );
        };
        long unreadCount = inAppNotificationRepository
                .countByRecipientAndReadAtIsNullAndCreatedAtGreaterThanEqual(user, createdAfter);

        return new InAppNotificationListResponse(
                notifications.map(InAppNotificationResponse::from),
                unreadCount
        );
    }

    @Transactional
    public InAppNotificationResponse markRead(Long notificationId) {
        Users user = getUserInfo();
        InAppNotification notification = inAppNotificationRepository
                .findByNotificationIdAndRecipient(notificationId, user)
                .orElseThrow(() -> new ApplicationException(
                        NotificationStatusCode.NOTIFICATION_NOT_FOUND
                ));

        notification.markRead();
        return InAppNotificationResponse.from(notification);
    }

    private Users getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return usersRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));
    }
}
