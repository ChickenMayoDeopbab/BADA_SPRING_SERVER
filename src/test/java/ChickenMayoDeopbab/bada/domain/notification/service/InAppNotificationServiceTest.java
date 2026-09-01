package ChickenMayoDeopbab.bada.domain.notification.service;

import ChickenMayoDeopbab.bada.domain.notification.dto.response.InAppNotificationListResponse;
import ChickenMayoDeopbab.bada.domain.notification.dto.response.InAppNotificationResponse;
import ChickenMayoDeopbab.bada.domain.notification.entity.InAppNotification;
import ChickenMayoDeopbab.bada.domain.notification.entity.InAppNotificationType;
import ChickenMayoDeopbab.bada.domain.notification.exception.NotificationStatusCode;
import ChickenMayoDeopbab.bada.domain.notification.model.NotificationFilter;
import ChickenMayoDeopbab.bada.domain.notification.repository.InAppNotificationRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InAppNotificationServiceTest {

    private final InAppNotificationRepository inAppNotificationRepository =
            mock(InAppNotificationRepository.class);
    private final UsersRepository usersRepository = mock(UsersRepository.class);
    private final InAppNotificationService service = new InAppNotificationService(
            inAppNotificationRepository,
            usersRepository
    );
    private final Users user = mock(Users.class);

    @BeforeEach
    void authenticate() {
        when(usersRepository.findByUsername("junha")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("junha", null)
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getNotificationsReturnsRecentPageAndUnreadCount() {
        PageRequest pageable = PageRequest.of(0, 20);
        InAppNotification notification = notification();
        when(inAppNotificationRepository
                .findByRecipientAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        eq(user),
                        any(LocalDateTime.class),
                        eq(pageable)
                ))
                .thenReturn(new PageImpl<>(List.of(notification), pageable, 1));
        when(inAppNotificationRepository
                .countByRecipientAndReadAtIsNullAndCreatedAtGreaterThanEqual(
                        eq(user),
                        any(LocalDateTime.class)
                ))
                .thenReturn(1L);

        LocalDateTime before = LocalDateTime.now().minusDays(14);
        InAppNotificationListResponse response = service.getNotifications(
                NotificationFilter.ALL,
                pageable
        );
        LocalDateTime after = LocalDateTime.now().minusDays(14);

        assertThat(response.notifications().getContent()).hasSize(1);
        assertThat(response.unreadCount()).isEqualTo(1L);
        ArgumentCaptor<LocalDateTime> createdAfter =
                ArgumentCaptor.forClass(LocalDateTime.class);
        verify(inAppNotificationRepository)
                .findByRecipientAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        eq(user),
                        createdAfter.capture(),
                        eq(pageable)
                );
        assertThat(createdAfter.getValue()).isBetween(before, after);
    }

    @Test
    void getNotificationsUsesUnreadQueryForUnreadFilter() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(inAppNotificationRepository
                .findByRecipientAndReadAtIsNullAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        eq(user),
                        any(LocalDateTime.class),
                        eq(pageable)
                ))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.getNotifications(NotificationFilter.UNREAD, pageable);

        verify(inAppNotificationRepository)
                .findByRecipientAndReadAtIsNullAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        eq(user),
                        any(LocalDateTime.class),
                        eq(pageable)
                );
    }

    @Test
    void markReadUpdatesOnlyOwnedNotification() {
        InAppNotification notification = notification();
        when(inAppNotificationRepository.findByNotificationIdAndRecipient(12L, user))
                .thenReturn(Optional.of(notification));

        InAppNotificationResponse response = service.markRead(12L);

        assertThat(response.read()).isTrue();
        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void markReadRejectsMissingOrOtherUsersNotification() {
        when(inAppNotificationRepository.findByNotificationIdAndRecipient(12L, user))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(12L))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getStatusCode())
                .isEqualTo(NotificationStatusCode.NOTIFICATION_NOT_FOUND);
    }

    private InAppNotification notification() {
        return InAppNotification.create(
                user,
                InAppNotificationType.POST_COMMENT,
                8L,
                "조상철",
                "profiles/actor.png",
                "조상철",
                "내 글에 댓글을 작성했어요.",
                10L,
                25L,
                null,
                "COMMENT:25"
        );
    }
}
