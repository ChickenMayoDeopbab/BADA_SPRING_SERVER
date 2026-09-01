package ChickenMayoDeopbab.bada.domain.notification.service;

import ChickenMayoDeopbab.bada.domain.notification.dto.request.CommunityNotificationRequest;
import ChickenMayoDeopbab.bada.domain.notification.entity.NotificationSetting;
import ChickenMayoDeopbab.bada.domain.notification.exception.NotificationStatusCode;
import ChickenMayoDeopbab.bada.domain.notification.model.CommunityNotificationType;
import ChickenMayoDeopbab.bada.domain.notification.model.PushMessage;
import ChickenMayoDeopbab.bada.domain.notification.port.PushMessageSender;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CommunityNotificationServiceTest {

    private final UsersRepository usersRepository = mock(UsersRepository.class);
    private final NotificationSettingRepository notificationSettingRepository =
            mock(NotificationSettingRepository.class);
    private final PushMessageSender pushMessageSender = mock(PushMessageSender.class);
    private final CommunityNotificationService service = new CommunityNotificationService(
            usersRepository,
            notificationSettingRepository,
            pushMessageSender
    );

    @Test
    void sendBuildsCommentNotificationWhenSettingsDoNotExist() {
        Users recipient = recipient(7L);
        when(usersRepository.findById(7L)).thenReturn(Optional.of(recipient));
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
    }

    @Test
    void sendBuildsReplyNotification() {
        Users recipient = recipient(7L);
        when(usersRepository.findById(7L)).thenReturn(Optional.of(recipient));
        when(notificationSettingRepository.findById(7L)).thenReturn(Optional.empty());

        service.send(request(CommunityNotificationType.REPLY, 7L, 8L));

        PushMessage message = captureMessage(recipient);
        assertThat(message.body()).isEqualTo("내 댓글에 새로운 답글이 달렸습니다.");
        assertThat(message.data().get("notificationType")).isEqualTo("COMMUNITY_REPLY");
    }

    @Test
    void sendSkipsSelfNotificationBeforeRepositoryLookup() {
        service.send(request(CommunityNotificationType.COMMENT, 7L, 7L));

        verifyNoInteractions(usersRepository, notificationSettingRepository, pushMessageSender);
    }

    @Test
    void sendSkipsNotificationWhenAllNotificationsAreDisabled() {
        Users recipient = recipient(7L);
        NotificationSetting setting = NotificationSetting.enabledByDefault(recipient);
        setting.update(false, true, true);
        when(usersRepository.findById(7L)).thenReturn(Optional.of(recipient));
        when(notificationSettingRepository.findById(7L)).thenReturn(Optional.of(setting));

        service.send(request(CommunityNotificationType.COMMENT, 7L, 8L));

        verifyNoInteractions(pushMessageSender);
    }

    @Test
    void sendSkipsNotificationWhenCommunityNotificationsAreDisabled() {
        Users recipient = recipient(7L);
        NotificationSetting setting = NotificationSetting.enabledByDefault(recipient);
        setting.update(true, false, true);
        when(usersRepository.findById(7L)).thenReturn(Optional.of(recipient));
        when(notificationSettingRepository.findById(7L)).thenReturn(Optional.of(setting));

        service.send(request(CommunityNotificationType.REPLY, 7L, 8L));

        verifyNoInteractions(pushMessageSender);
    }

    @Test
    void sendRejectsUnknownRecipient() {
        when(usersRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.send(request(CommunityNotificationType.COMMENT, 7L, 8L)))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getStatusCode())
                .isEqualTo(NotificationStatusCode.RECIPIENT_NOT_FOUND);

        verifyNoInteractions(notificationSettingRepository, pushMessageSender);
    }

    private PushMessage captureMessage(Users recipient) {
        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushMessageSender).send(eq(recipient), captor.capture());
        return captor.getValue();
    }

    private Users recipient(Long userId) {
        Users recipient = mock(Users.class);
        when(recipient.getUserId()).thenReturn(userId);
        return recipient;
    }

    private CommunityNotificationRequest request(
            CommunityNotificationType type,
            Long recipientUserId,
            Long actorUserId
    ) {
        return new CommunityNotificationRequest(type, recipientUserId, actorUserId, 10L, 25L);
    }
}
