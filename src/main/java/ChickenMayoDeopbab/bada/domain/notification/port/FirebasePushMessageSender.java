package ChickenMayoDeopbab.bada.domain.notification.port;

import ChickenMayoDeopbab.bada.domain.notification.entity.PushDevice;
import ChickenMayoDeopbab.bada.domain.notification.model.PushMessage;
import ChickenMayoDeopbab.bada.domain.notification.repository.PushDeviceRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "true")
public class FirebasePushMessageSender implements PushMessageSender {

    private final FirebaseMessaging firebaseMessaging;
    private final PushDeviceRepository pushDeviceRepository;

    @Override
    public void send(Users recipient, PushMessage pushMessage) {
        List<PushDevice> devices = pushDeviceRepository.findAllByUser(recipient);
        if (devices.isEmpty()) {
            log.info(
                    "등록된 푸시 기기가 없어 알림 발송 생략 userId={} notificationType={}",
                    recipient.getUserId(),
                    pushMessage.data().get("notificationType")
            );
            return;
        }

        List<Message> messages = devices.stream()
                .map(device -> createMessage(device, pushMessage))
                .toList();

        try {
            BatchResponse response = firebaseMessaging.sendEach(messages);
            removeUnregisteredDevices(devices, response.getResponses());
            log.info(
                    "푸시 알림 발송 완료 userId={} notificationType={} successCount={} failureCount={}",
                    recipient.getUserId(),
                    pushMessage.data().get("notificationType"),
                    response.getSuccessCount(),
                    response.getFailureCount()
            );
        } catch (FirebaseMessagingException e) {
            // 푸시 제공자 장애가 알림을 요청한 비즈니스 트랜잭션을 롤백시키지 않게 한다.
            log.error(
                    "푸시 알림 일괄 발송 실패 userId={} notificationType={} errorCode={}",
                    recipient.getUserId(),
                    pushMessage.data().get("notificationType"),
                    e.getMessagingErrorCode()
            );
        }
    }

    @SuppressWarnings("deprecation")
    private Message createMessage(PushDevice device, PushMessage pushMessage) {
        // 현재 앱 계약은 FCM registration token 기반이다. 앱이 FID를 전달할 때 setFid로 함께 전환한다.
        return Message.builder()
                .setToken(device.getToken())
                .setNotification(Notification.builder()
                        .setTitle(pushMessage.title())
                        .setBody(pushMessage.body())
                        .build())
                .putAllData(pushMessage.data())
                .build();
    }

    private void removeUnregisteredDevices(
            List<PushDevice> devices,
            List<SendResponse> responses
    ) {
        List<PushDevice> unregisteredDevices = new ArrayList<>();
        for (int index = 0; index < responses.size(); index++) {
            SendResponse response = responses.get(index);
            if (isUnregistered(response)) {
                unregisteredDevices.add(devices.get(index));
            }
        }

        if (!unregisteredDevices.isEmpty()) {
            pushDeviceRepository.deleteAll(unregisteredDevices);
            log.info("만료된 FCM 디바이스 토큰 정리 count={}", unregisteredDevices.size());
        }
    }

    private boolean isUnregistered(SendResponse response) {
        return !response.isSuccessful()
                && response.getException() != null
                && response.getException().getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED;
    }
}
