package ChickenMayoDeopbab.bada.domain.trainingcallschedule.port;

import ChickenMayoDeopbab.bada.domain.notification.entity.PushDevice;
import ChickenMayoDeopbab.bada.domain.notification.repository.NotificationSettingRepository;
import ChickenMayoDeopbab.bada.domain.notification.repository.PushDeviceRepository;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallSchedule;
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
public class FirebasePushNotificationAdapter implements PushNotificationPort {

    private static final String TITLE = "바다";
    private static final String BODY = "훈련 전화가 왔습니다.";
    private static final String NOTIFICATION_TYPE = "TRAINING_CALL";

    private final FirebaseMessaging firebaseMessaging;
    private final PushDeviceRepository pushDeviceRepository;
    private final NotificationSettingRepository notificationSettingRepository;

    @Override
    public void notifyIncomingCall(TrainingCallSchedule schedule) {
        Long userId = schedule.getUser().getUserId();
        if (!allowsTrainingNotification(userId)) {
            log.info("훈련 알림 설정 비활성화로 발송 생략 scheduleId={} userId={}", schedule.getScheduleId(), userId);
            return;
        }

        List<PushDevice> devices = pushDeviceRepository.findAllByUser(schedule.getUser());
        if (devices.isEmpty()) {
            log.info("등록된 푸시 기기가 없어 훈련 알림 발송 생략 scheduleId={} userId={}", schedule.getScheduleId(), userId);
            return;
        }

        List<Message> messages = devices.stream()
                .map(device -> createMessage(device, schedule))
                .toList();

        try {
            BatchResponse response = firebaseMessaging.sendEach(messages);
            removeUnregisteredDevices(devices, response.getResponses());
            log.info(
                    "훈련 알림 발송 완료 scheduleId={} userId={} successCount={} failureCount={}",
                    schedule.getScheduleId(),
                    userId,
                    response.getSuccessCount(),
                    response.getFailureCount()
            );
        } catch (FirebaseMessagingException e) {
            // 푸시 제공자 장애가 이미 생성된 훈련 세션과 예약 상태를 롤백시키지 않게 한다.
            log.error(
                    "훈련 알림 일괄 발송 실패 scheduleId={} userId={} errorCode={}",
                    schedule.getScheduleId(),
                    userId,
                    e.getMessagingErrorCode()
            );
        }
    }

    private boolean allowsTrainingNotification(Long userId) {
        return notificationSettingRepository.findById(userId)
                .map(setting -> setting.allowsTrainingNotification())
                .orElse(true);
    }

    @SuppressWarnings("deprecation")
    private Message createMessage(PushDevice device, TrainingCallSchedule schedule) {
        // 현재 앱 계약은 FCM registration token 기반이다. 앱이 FID를 전달할 때 setFid로 함께 전환한다.
        return Message.builder()
                .setToken(device.getToken())
                .setNotification(Notification.builder()
                        .setTitle(TITLE)
                        .setBody(BODY)
                        .build())
                .putData("notificationType", NOTIFICATION_TYPE)
                .putData("scheduleId", String.valueOf(schedule.getScheduleId()))
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
