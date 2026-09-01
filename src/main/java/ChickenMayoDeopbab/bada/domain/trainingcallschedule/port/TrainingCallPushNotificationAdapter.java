package ChickenMayoDeopbab.bada.domain.trainingcallschedule.port;

import ChickenMayoDeopbab.bada.domain.notification.model.PushMessage;
import ChickenMayoDeopbab.bada.domain.notification.port.PushMessageSender;
import ChickenMayoDeopbab.bada.domain.notification.repository.NotificationSettingRepository;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingCallPushNotificationAdapter implements PushNotificationPort {

    private static final String TITLE = "바다";
    private static final String BODY = "훈련 전화가 왔습니다.";
    private static final String NOTIFICATION_TYPE = "TRAINING_CALL";

    private final PushMessageSender pushMessageSender;
    private final NotificationSettingRepository notificationSettingRepository;

    @Override
    public void notifyIncomingCall(TrainingCallSchedule schedule) {
        Long userId = schedule.getUser().getUserId();
        if (!allowsTrainingNotification(userId)) {
            log.info("훈련 알림 설정 비활성화로 발송 생략 scheduleId={} userId={}", schedule.getScheduleId(), userId);
            return;
        }

        PushMessage pushMessage = new PushMessage(
                TITLE,
                BODY,
                Map.of(
                        "notificationType", NOTIFICATION_TYPE,
                        "scheduleId", String.valueOf(schedule.getScheduleId())
                )
        );
        pushMessageSender.send(schedule.getUser(), pushMessage);
    }

    private boolean allowsTrainingNotification(Long userId) {
        return notificationSettingRepository.findById(userId)
                .map(setting -> setting.allowsTrainingNotification())
                .orElse(true);
    }
}
