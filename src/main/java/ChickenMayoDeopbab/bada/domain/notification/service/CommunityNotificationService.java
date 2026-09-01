package ChickenMayoDeopbab.bada.domain.notification.service;

import ChickenMayoDeopbab.bada.domain.notification.dto.request.CommunityNotificationRequest;
import ChickenMayoDeopbab.bada.domain.notification.entity.NotificationSetting;
import ChickenMayoDeopbab.bada.domain.notification.exception.NotificationStatusCode;
import ChickenMayoDeopbab.bada.domain.notification.model.PushMessage;
import ChickenMayoDeopbab.bada.domain.notification.port.PushMessageSender;
import ChickenMayoDeopbab.bada.domain.notification.repository.NotificationSettingRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityNotificationService {

    private static final String TITLE = "바다";

    private final UsersRepository usersRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final PushMessageSender pushMessageSender;

    public void send(CommunityNotificationRequest request) {
        if (request.recipientUserId().equals(request.actorUserId())) {
            log.info(
                    "커뮤니티 본인 알림 발송 생략 userId={} postId={} commentId={}",
                    request.actorUserId(),
                    request.postId(),
                    request.commentId()
            );
            return;
        }

        Users recipient = usersRepository.findById(request.recipientUserId())
                .orElseThrow(() -> ApplicationException.of(NotificationStatusCode.RECIPIENT_NOT_FOUND));
        if (!allowsCommunityNotification(recipient.getUserId())) {
            log.info(
                    "커뮤니티 알림 설정 비활성화로 발송 생략 userId={} postId={} commentId={}",
                    recipient.getUserId(),
                    request.postId(),
                    request.commentId()
            );
            return;
        }

        PushMessage pushMessage = new PushMessage(
                TITLE,
                request.type().body(),
                Map.of(
                        "notificationType", request.type().notificationType(),
                        "postId", String.valueOf(request.postId()),
                        "commentId", String.valueOf(request.commentId())
                )
        );
        pushMessageSender.send(recipient, pushMessage);
    }

    private boolean allowsCommunityNotification(Long userId) {
        return notificationSettingRepository.findById(userId)
                .map(NotificationSetting::allowsCommunityNotification)
                .orElse(true);
    }
}
