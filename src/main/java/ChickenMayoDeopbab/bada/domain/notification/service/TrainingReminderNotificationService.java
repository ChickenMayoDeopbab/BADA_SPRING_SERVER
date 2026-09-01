package ChickenMayoDeopbab.bada.domain.notification.service;

import ChickenMayoDeopbab.bada.domain.notification.entity.InAppNotification;
import ChickenMayoDeopbab.bada.domain.notification.entity.InAppNotificationType;
import ChickenMayoDeopbab.bada.domain.notification.entity.NotificationSetting;
import ChickenMayoDeopbab.bada.domain.notification.repository.InAppNotificationRepository;
import ChickenMayoDeopbab.bada.domain.notification.repository.NotificationSettingRepository;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallSchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrainingReminderNotificationService {

    private static final String TITLE = "시나리오 훈련";

    private final NotificationSettingRepository notificationSettingRepository;
    private final InAppNotificationRepository inAppNotificationRepository;

    public void saveFor(TrainingCallSchedule schedule) {
        if (!allowsTrainingNotification(schedule.getUser().getUserId())) {
            return;
        }

        String eventKey = "TRAINING_REMINDER:" + schedule.getScheduleId();
        if (inAppNotificationRepository.existsByEventKey(eventKey)) {
            return;
        }

        inAppNotificationRepository.saveAndFlush(InAppNotification.create(
                schedule.getUser(),
                InAppNotificationType.TRAINING_REMINDER,
                null,
                null,
                null,
                TITLE,
                reminderMessage(
                        schedule.getMinDelayMinutes(),
                        schedule.getMaxDelayMinutes()
                ),
                null,
                null,
                schedule.getScheduleId(),
                eventKey
        ));
    }

    static String reminderMessage(int minDelayMinutes, int maxDelayMinutes) {
        if (minDelayMinutes == maxDelayMinutes) {
            return minDelayMinutes + "분 후 시나리오 훈련이 시작돼요.";
        }
        return minDelayMinutes + "분~" + maxDelayMinutes
                + "분 후 시나리오 훈련이 시작돼요.";
    }

    private boolean allowsTrainingNotification(Long userId) {
        return notificationSettingRepository.findById(userId)
                .map(NotificationSetting::allowsTrainingNotification)
                .orElse(true);
    }
}
