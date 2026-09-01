package ChickenMayoDeopbab.bada.domain.notification.service;

import ChickenMayoDeopbab.bada.domain.notification.dto.request.CommunityNotificationRequest;
import ChickenMayoDeopbab.bada.domain.notification.entity.InAppNotification;
import ChickenMayoDeopbab.bada.domain.notification.entity.InAppNotificationType;
import ChickenMayoDeopbab.bada.domain.notification.entity.NotificationSetting;
import ChickenMayoDeopbab.bada.domain.notification.exception.NotificationStatusCode;
import ChickenMayoDeopbab.bada.domain.notification.model.CommunityNotificationType;
import ChickenMayoDeopbab.bada.domain.notification.model.CommunityReactionKind;
import ChickenMayoDeopbab.bada.domain.notification.model.PushMessage;
import ChickenMayoDeopbab.bada.domain.notification.port.PushMessageSender;
import ChickenMayoDeopbab.bada.domain.notification.repository.InAppNotificationRepository;
import ChickenMayoDeopbab.bada.domain.notification.repository.NotificationSettingRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CommunityNotificationServiceTest {

    private final UsersRepository usersRepository = mock(UsersRepository.class);
    private final NotificationSettingRepository notificationSettingRepository =
            mock(NotificationSettingRepository.class);
    private final InAppNotificationRepository inAppNotificationRepository =
            mock(InAppNotificationRepository.class);
    private final PushMessageSender pushMessageSender = mock(PushMessageSender.class);
    private final CommunityNotificationService service = new CommunityNotificationService(
            usersRepository,
            notificationSettingRepository,
            inAppNotificationRepository,
            pushMessageSender
    );

    @Test
    void sendBuildsCommentNotificationWhenSettingsDoNotExist() {
        Users recipient = recipient(7L);
        Users actor = actor(8L);
        when(usersRepository.findById(7L)).thenReturn(Optional.of(recipient));
        when(usersRepository.findById(8L)).thenReturn(Optional.of(actor));
        when(notificationSettingRepository.findById(7L)).thenReturn(Optional.empty());

        service.send(request(CommunityNotificationType.COMMENT, 7L, 8L));

        PushMessage message = captureMessage(recipient);
        assertThat(message.title()).isEqualTo("바다");
        assertThat(message.body()).isEqualTo("내 게시글에 새로운 댓글이 달렸습니다.");
        assertThat(message.data()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "notificationType", "COMMUNITY_COMMENT",
                "postId", "10",
                "commentId", "25"
        ));

        InAppNotification notification = captureInAppNotification();
        assertThat(notification.getType()).isEqualTo(InAppNotificationType.POST_COMMENT);
        assertThat(notification.getTitle()).isEqualTo("조상철");
        assertThat(notification.getMessage()).isEqualTo("내 글에 댓글을 작성했어요.");
        assertThat(notification.getPostId()).isEqualTo(10L);
        assertThat(notification.getCommentId()).isEqualTo(25L);
    }

    @Test
    void sendBuildsReplyNotification() {
        Users recipient = recipient(7L);
        Users actor = actor(8L);
        when(usersRepository.findById(7L)).thenReturn(Optional.of(recipient));
        when(usersRepository.findById(8L)).thenReturn(Optional.of(actor));
        when(notificationSettingRepository.findById(7L)).thenReturn(Optional.empty());

        service.send(request(CommunityNotificationType.REPLY, 7L, 8L));

        PushMessage message = captureMessage(recipient);
        assertThat(message.body()).isEqualTo("내 댓글에 새로운 답글이 달렸습니다.");
        assertThat(message.data().get("notificationType")).isEqualTo("COMMUNITY_REPLY");
    }

    @Test
    void sendSkipsSelfNotificationBeforeRepositoryLookup() {
        service.send(request(CommunityNotificationType.COMMENT, 7L, 7L));

        verifyNoInteractions(
                usersRepository,
                notificationSettingRepository,
                inAppNotificationRepository,
                pushMessageSender
        );
    }

    @Test
    void sendSkipsNotificationWhenAllNotificationsAreDisabled() {
        Users recipient = recipient(7L);
        NotificationSetting setting = NotificationSetting.enabledByDefault(recipient);
        setting.update(false, true, true);
        when(usersRepository.findById(7L)).thenReturn(Optional.of(recipient));
        when(notificationSettingRepository.findById(7L)).thenReturn(Optional.of(setting));

        service.send(request(CommunityNotificationType.COMMENT, 7L, 8L));

        verifyNoInteractions(inAppNotificationRepository, pushMessageSender);
    }

    @Test
    void sendSkipsNotificationWhenCommunityNotificationsAreDisabled() {
        Users recipient = recipient(7L);
        NotificationSetting setting = NotificationSetting.enabledByDefault(recipient);
        setting.update(true, false, true);
        when(usersRepository.findById(7L)).thenReturn(Optional.of(recipient));
        when(notificationSettingRepository.findById(7L)).thenReturn(Optional.of(setting));

        service.send(request(CommunityNotificationType.REPLY, 7L, 8L));

        verifyNoInteractions(inAppNotificationRepository, pushMessageSender);
    }

    @Test
    void sendRejectsUnknownRecipient() {
        when(usersRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.send(request(CommunityNotificationType.COMMENT, 7L, 8L)))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getStatusCode())
                .isEqualTo(NotificationStatusCode.RECIPIENT_NOT_FOUND);

        verifyNoInteractions(
                notificationSettingRepository,
                inAppNotificationRepository,
                pushMessageSender
        );
    }

    @Test
    void sendBuildsReactionNotificationForNewReaction() {
        Users recipient = recipient(7L);
        Users actor = actor(8L);
        when(usersRepository.findById(7L)).thenReturn(Optional.of(recipient));
        when(usersRepository.findById(8L)).thenReturn(Optional.of(actor));
        when(notificationSettingRepository.findById(7L)).thenReturn(Optional.empty());

        service.send(new CommunityNotificationRequest(
                CommunityNotificationType.REACTION,
                7L,
                8L,
                10L,
                null,
                31L,
                CommunityReactionKind.LIKE
        ));

        PushMessage message = captureMessage(recipient);
        assertThat(message.body()).isEqualTo("내 글에 좋아요를 눌렀어요.");
        assertThat(message.data()).containsEntry("reactionId", "31");
        assertThat(message.data()).containsEntry("reactionKind", "LIKE");

        InAppNotification notification = captureInAppNotification();
        assertThat(notification.getType()).isEqualTo(InAppNotificationType.POST_LIKE);
        assertThat(notification.getMessage()).isEqualTo("내 글에 좋아요를 눌렀어요.");
    }

    @Test
    void sendSkipsAlreadyStoredEvent() {
        Users recipient = recipient(7L);
        Users actor = actor(8L);
        when(usersRepository.findById(7L)).thenReturn(Optional.of(recipient));
        when(usersRepository.findById(8L)).thenReturn(Optional.of(actor));
        when(notificationSettingRepository.findById(7L)).thenReturn(Optional.empty());
        when(inAppNotificationRepository.existsByEventKey("COMMENT:25"))
                .thenReturn(true);

        service.send(request(CommunityNotificationType.COMMENT, 7L, 8L));

        verify(inAppNotificationRepository, never()).saveAndFlush(any());
        verifyNoInteractions(pushMessageSender);
    }

    private PushMessage captureMessage(Users recipient) {
        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushMessageSender).send(eq(recipient), captor.capture());
        return captor.getValue();
    }

    private InAppNotification captureInAppNotification() {
        ArgumentCaptor<InAppNotification> captor =
                ArgumentCaptor.forClass(InAppNotification.class);
        verify(inAppNotificationRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private Users recipient(Long userId) {
        Users recipient = mock(Users.class);
        when(recipient.getUserId()).thenReturn(userId);
        return recipient;
    }

    private Users actor(Long userId) {
        Users actor = mock(Users.class);
        when(actor.getUserId()).thenReturn(userId);
        when(actor.getName()).thenReturn("조상철");
        when(actor.getProfileImage()).thenReturn("profiles/actor.png");
        return actor;
    }

    private CommunityNotificationRequest request(
            CommunityNotificationType type,
            Long recipientUserId,
            Long actorUserId
    ) {
        return new CommunityNotificationRequest(type, recipientUserId, actorUserId, 10L, 25L);
    }
}
