package ChickenMayoDeopbab.bada.domain.trainingcallschedule.service;

import ChickenMayoDeopbab.bada.domain.session.dto.request.CreateSessionRequest;
import ChickenMayoDeopbab.bada.domain.session.dto.response.CreateSessionResponse;
import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.session.service.SessionService;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallSchedule;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallScheduleStatus;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.port.PushNotificationPort;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.repository.TrainingCallScheduleRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.global.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TrainingCallScheduleTriggerServiceTest {

    private final TrainingCallScheduleRepository trainingCallScheduleRepository =
            mock(TrainingCallScheduleRepository.class);
    private final PushNotificationPort pushNotificationPort = mock(PushNotificationPort.class);
    private final SessionService sessionService = mock(SessionService.class);
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final TrainingCallScheduleTriggerService service = new TrainingCallScheduleTriggerService(
            trainingCallScheduleRepository,
            pushNotificationPort,
            sessionService,
            jwtProvider
    );

    @Test
    void triggerDueSchedulesCreatesVoiceSessionAndSendsNotification() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 6, 16, 32);
        Users user = mock(Users.class);
        when(user.getUserId()).thenReturn(7L);
        TrainingCallSchedule schedule = schedule(user, now.minusMinutes(1));
        when(trainingCallScheduleRepository.findTop100ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                TrainingCallScheduleStatus.SCHEDULED,
                now
        )).thenReturn(List.of(schedule));
        when(jwtProvider.createAccessToken(eq(7L), any())).thenReturn("scheduled-token");
        when(sessionService.createForUser(eq(user), any(CreateSessionRequest.class), eq("scheduled-token")))
                .thenReturn(new CreateSessionResponse("session-id", "ws://voice"));

        int count = service.triggerDueSchedules(now);

        assertThat(count).isEqualTo(1);
        assertThat(schedule.getStatus()).isEqualTo(TrainingCallScheduleStatus.RINGING);
        assertThat(schedule.getTriggeredAt()).isEqualTo(now);
        assertThat(schedule.getSessionId()).isEqualTo("session-id");
        assertThat(schedule.getWsUrl()).isEqualTo("ws://voice");

        ArgumentCaptor<CreateSessionRequest> captor = ArgumentCaptor.forClass(CreateSessionRequest.class);
        verify(sessionService).createForUser(eq(user), captor.capture(), eq("scheduled-token"));
        CreateSessionRequest request = captor.getValue();
        assertThat(request.scenarioId()).isEqualTo(3L);
        assertThat(request.type()).isEqualTo(SessionType.SCENARIO);
        assertThat(request.aiPersonality()).isEqualTo(AiPersonality.NORMAL);
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

    private TrainingCallSchedule schedule(Users user, LocalDateTime scheduledAt) {
        return TrainingCallSchedule.builder()
                .user(user)
                .scenarioId(3L)
                .type(SessionType.SCENARIO)
                .aiPersonality(AiPersonality.NORMAL)
                .minDelayMinutes(0)
                .maxDelayMinutes(180)
                .scheduledAt(scheduledAt)
                .build();
    }
}
