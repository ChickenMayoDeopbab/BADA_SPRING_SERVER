package ChickenMayoDeopbab.bada.domain.trainingcallschedule.service;

import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallSchedule;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallScheduleStatus;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.port.PushNotificationPort;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.repository.TrainingCallScheduleRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TrainingCallScheduleTriggerServiceTest {

    private final TrainingCallScheduleRepository trainingCallScheduleRepository =
            mock(TrainingCallScheduleRepository.class);
    private final PushNotificationPort pushNotificationPort = mock(PushNotificationPort.class);
    private final TrainingCallScheduleTriggerService service = new TrainingCallScheduleTriggerService(
            trainingCallScheduleRepository,
            pushNotificationPort
    );

    @Test
    void triggerDueSchedulesMarksDueSchedulesRingingAndSendsNotification() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 6, 16, 32);
        TrainingCallSchedule schedule = schedule(now.minusMinutes(1));
        when(trainingCallScheduleRepository.findTop100ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                TrainingCallScheduleStatus.SCHEDULED,
                now
        )).thenReturn(List.of(schedule));

        int count = service.triggerDueSchedules(now);

        assertThat(count).isEqualTo(1);
        assertThat(schedule.getStatus()).isEqualTo(TrainingCallScheduleStatus.RINGING);
        assertThat(schedule.getTriggeredAt()).isEqualTo(now);
        verify(pushNotificationPort).notifyIncomingCall(schedule);
    }

    @Test
    void triggerDueSchedulesDoesNothingWhenNoSchedulesAreDue() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 6, 16, 32);
        when(trainingCallScheduleRepository.findTop100ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                TrainingCallScheduleStatus.SCHEDULED,
                now
        )).thenReturn(List.of());

        int count = service.triggerDueSchedules(now);

        assertThat(count).isZero();
        verifyNoInteractions(pushNotificationPort);
    }

    private TrainingCallSchedule schedule(LocalDateTime scheduledAt) {
        return TrainingCallSchedule.builder()
                .user(mock(Users.class))
                .scenarioId(3L)
                .type(SessionType.SCENARIO)
                .aiPersonality(AiPersonality.NORMAL)
                .minDelayMinutes(0)
                .maxDelayMinutes(180)
                .scheduledAt(scheduledAt)
                .build();
    }
}
