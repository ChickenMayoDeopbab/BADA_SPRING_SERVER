package ChickenMayoDeopbab.bada.domain.notification.service;

import ChickenMayoDeopbab.bada.domain.notification.dto.request.CommunityNotificationRequest;
import ChickenMayoDeopbab.bada.domain.notification.entity.InAppNotification;
import ChickenMayoDeopbab.bada.domain.notification.entity.InAppNotificationType;
import ChickenMayoDeopbab.bada.domain.notification.entity.NotificationSetting;
import ChickenMayoDeopbab.bada.domain.notification.exception.NotificationStatusCode;
import ChickenMayoDeopbab.bada.domain.notification.model.PushMessage;
import ChickenMayoDeopbab.bada.domain.notification.port.PushMessageSender;
import ChickenMayoDeopbab.bada.domain.notification.repository.InAppNotificationRepository;
import ChickenMayoDeopbab.bada.domain.notification.repository.NotificationSettingRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityNotificationService {

    private static final String TITLE = "바다";

    private final UsersRepository usersRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final InAppNotificationRepository inAppNotificationRepository;
    private final PushMessageSender pushMessageSender;

    @Transactional
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

        Users actor = usersRepository.findById(request.actorUserId())
                .orElseThrow(() -> ApplicationException.of(NotificationStatusCode.ACTOR_NOT_FOUND));
        if (!saveInAppNotification(request, recipient, actor)) {
            log.info(
                    "중복 커뮤니티 알림 처리 생략 eventKey={}",
                    eventKey(request)
            );
            return;
        }

        String pushBody = pushBody(request);
        Map<String, String> data = new HashMap<>();
        data.put("notificationType", request.type().notificationType());
        data.put("postId", String.valueOf(request.postId()));
        if (request.commentId() != null) {
            data.put("commentId", String.valueOf(request.commentId()));
        }
        if (request.reactionId() != null) {
            data.put("reactionId", String.valueOf(request.reactionId()));
            data.put("reactionKind", request.reactionKind().name());
        }

        PushMessage pushMessage = new PushMessage(
                TITLE,
                pushBody,
                Map.copyOf(data)
        );
        pushMessageSender.send(recipient, pushMessage);
    }

    private boolean saveInAppNotification(
            CommunityNotificationRequest request,
            Users recipient,
            Users actor
    ) {
        String eventKey = eventKey(request);
        if (inAppNotificationRepository.existsByEventKey(eventKey)) {
            return false;
        }

        String actorName = actorName(actor);
        inAppNotificationRepository.saveAndFlush(InAppNotification.create(
                recipient,
                inAppNotificationType(request),
                actor.getUserId(),
                actorName,
                actor.getProfileImage(),
                actorName,
                message(request),
                request.postId(),
                request.commentId(),
                null,
                eventKey
        ));
        return true;
    }

    private String actorName(Users actor) {
        if (actor.getName() == null || actor.getName().isBlank()) {
            return "사용자";
        }
        return actor.getName();
    }

    private InAppNotificationType inAppNotificationType(
            CommunityNotificationRequest request
    ) {
        return switch (request.type()) {
            case COMMENT -> InAppNotificationType.POST_COMMENT;
            case REPLY -> InAppNotificationType.COMMENT_REPLY;
            case REACTION -> request.reactionKind().inAppNotificationType();
        };
    }

    private String message(CommunityNotificationRequest request) {
        return switch (request.type()) {
            case COMMENT -> "내 글에 댓글을 작성했어요.";
            case REPLY -> "내 댓글에 답글을 작성했어요.";
            case REACTION -> request.reactionKind().message();
        };
    }

    private String pushBody(CommunityNotificationRequest request) {
        if (request.type().body() != null) {
            return request.type().body();
        }
        return request.reactionKind().message();
    }

    private String eventKey(CommunityNotificationRequest request) {
        return switch (request.type()) {
            case COMMENT -> "COMMENT:" + request.commentId();
            case REPLY -> "REPLY:" + request.commentId();
            case REACTION -> "REACTION:" + request.reactionId();
        };
    }

    private boolean allowsCommunityNotification(Long userId) {
        return notificationSettingRepository.findById(userId)
                .map(NotificationSetting::allowsCommunityNotification)
                .orElse(true);
    }
}
