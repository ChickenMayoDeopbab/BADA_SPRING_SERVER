package ChickenMayoDeopbab.bada.domain.notification.service;

import ChickenMayoDeopbab.bada.domain.notification.entity.InAppNotification;
import ChickenMayoDeopbab.bada.domain.notification.entity.InAppNotificationType;
import ChickenMayoDeopbab.bada.domain.notification.entity.NotificationSetting;
import ChickenMayoDeopbab.bada.domain.notification.repository.InAppNotificationRepository;
import ChickenMayoDeopbab.bada.domain.notification.repository.NotificationSettingRepository;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallSchedule;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TrainingReminderNotificationServiceTest {

    private final NotificationSettingRepository notificationSettingRepository =
            mock(NotificationSettingRepository.class);
    private final InAppNotificationRepository inAppNotificationRepository =
            mock(InAppNotificationRepository.class);
    private final TrainingReminderNotificationService service =
            new TrainingReminderNotificationService(
                    notificationSettingRepository,
                    inAppNotificationRepository
            );

    @Test
    void saveForStoresRequestedDelayRangeMessageByDefault() {
        Users user = user(7L);
        TrainingCallSchedule schedule = schedule(user, 15L, 3, 6);
        when(notificationSettingRepository.findById(7L)).thenReturn(Optional.empty());

        service.saveFor(schedule);

        ArgumentCaptor<InAppNotification> captor =
                ArgumentCaptor.forClass(InAppNotification.class);
        verify(inAppNotificationRepository).saveAndFlush(captor.capture());
        InAppNotification notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(InAppNotificationType.TRAINING_REMINDER);
        assertThat(notification.getTitle()).isEqualTo("시나리오 훈련");
        assertThat(notification.getMessage())
                .isEqualTo("3분~6분 후 시나리오 훈련이 시작돼요.");
        assertThat(notification.getScheduleId()).isEqualTo(15L);
    }

    @Test
    void reminderMessageUsesSingleDelayWhenRangeIsFixed() {
        assertThat(TrainingReminderNotificationService.reminderMessage(3, 3))
                .isEqualTo("3분 후 시나리오 훈련이 시작돼요.");
    }

    @Test
    void saveForSkipsWhenTrainingNotificationsAreDisabled() {
        Users user = user(7L);
        NotificationSetting setting = NotificationSetting.enabledByDefault(user);
        setting.update(true, true, false);
        when(notificationSettingRepository.findById(7L)).thenReturn(Optional.of(setting));

        service.saveFor(schedule(user, 15L, 3, 6));

        verifyNoInteractions(inAppNotificationRepository);
    }

    @Test
    void saveForSkipsDuplicateScheduleEvent() {
        Users user = user(7L);
        when(notificationSettingRepository.findById(7L)).thenReturn(Optional.empty());
        when(inAppNotificationRepository.existsByEventKey("TRAINING_REMINDER:15"))
                .thenReturn(true);

        service.saveFor(schedule(user, 15L, 3, 6));

        verify(inAppNotificationRepository, never()).saveAndFlush(any());
    }

    private Users user(Long userId) {
        Users user = mock(Users.class);
        when(user.getUserId()).thenReturn(userId);
        return user;
    }

    private TrainingCallSchedule schedule(
            Users user,
            Long scheduleId,
            int minDelayMinutes,
            int maxDelayMinutes
    ) {
        TrainingCallSchedule schedule = mock(TrainingCallSchedule.class);
        when(schedule.getUser()).thenReturn(user);
        when(schedule.getScheduleId()).thenReturn(scheduleId);
        when(schedule.getMinDelayMinutes()).thenReturn(minDelayMinutes);
        when(schedule.getMaxDelayMinutes()).thenReturn(maxDelayMinutes);
        return schedule;
    }
}
