package ChickenMayoDeopbab.bada.domain.trainingcallschedule.service;

import ChickenMayoDeopbab.bada.domain.session.dto.request.CreateSessionRequest;
import ChickenMayoDeopbab.bada.domain.session.dto.response.CreateSessionResponse;
import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.session.service.SessionService;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.dto.request.CreateTrainingCallScheduleRequest;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.dto.response.AcceptTrainingCallScheduleResponse;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.dto.response.TrainingCallScheduleResponse;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallSchedule;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallScheduleStatus;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.exception.TrainingCallScheduleStatusCode;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.repository.TrainingCallScheduleRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrainingCallScheduleServiceTest {

    private final UsersRepository usersRepository = mock(UsersRepository.class);
    private final TrainingCallScheduleRepository trainingCallScheduleRepository =
            mock(TrainingCallScheduleRepository.class);
    private final SessionService sessionService = mock(SessionService.class);
    private final TrainingCallScheduleService service = new TrainingCallScheduleService(
            usersRepository,
            trainingCallScheduleRepository,
            sessionService
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolveScheduledAtWithZeroRangeReturnsNow() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 6, 16, 32);

        LocalDateTime scheduledAt = TrainingCallScheduleService.resolveScheduledAt(now, 0, 0);

        assertThat(scheduledAt).isEqualTo(now);
    }

    @Test
    void resolveScheduledAtUsesRandomDelayWithinRange() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 6, 16, 32);

        for (int i = 0; i < 100; i++) {
            LocalDateTime scheduledAt = TrainingCallScheduleService.resolveScheduledAt(now, 0, 180);

            assertThat(scheduledAt).isBetween(now, now.plusMinutes(180));
        }
    }

    @Test
    void createRejectsInvalidDelayRange() {
        assertThatThrownBy(() -> TrainingCallScheduleService.validateDelayRange(10, 3))
                .isInstanceOf(ApplicationException.class)
                .extracting(ex -> ((ApplicationException) ex).getStatusCode())
                .isEqualTo(TrainingCallScheduleStatusCode.INVALID_DELAY_RANGE);
    }

    @Test
    void createSavesScheduleInRequestedRange() {
        Users user = givenAuthenticatedUser();
        when(trainingCallScheduleRepository.save(any(TrainingCallSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        TrainingCallScheduleResponse response = service.create(new CreateTrainingCallScheduleRequest(
                3L,
                SessionType.SCENARIO,
                AiPersonality.NORMAL,
                0,
                180,
                null
        ));
        LocalDateTime after = LocalDateTime.now().plusMinutes(180);

        ArgumentCaptor<TrainingCallSchedule> captor = ArgumentCaptor.forClass(TrainingCallSchedule.class);
        verify(trainingCallScheduleRepository).save(captor.capture());
        TrainingCallSchedule schedule = captor.getValue();
        assertThat(schedule.getUser()).isEqualTo(user);
        assertThat(schedule.getScenarioId()).isEqualTo(3L);
        assertThat(schedule.getStatus()).isEqualTo(TrainingCallScheduleStatus.SCHEDULED);
        assertThat(schedule.getScheduledAt()).isBetween(before.minusSeconds(1), after.plusSeconds(1));
        assertThat(response.status()).isEqualTo(TrainingCallScheduleStatus.SCHEDULED);
    }

    @Test
    void cancelChangesScheduledScheduleToCanceled() {
        Users user = givenAuthenticatedUser();
        TrainingCallSchedule schedule = schedule(user);
        when(trainingCallScheduleRepository.findByScheduleIdAndUser(1L, user)).thenReturn(Optional.of(schedule));

        TrainingCallScheduleResponse response = service.cancel(1L);

        assertThat(schedule.getStatus()).isEqualTo(TrainingCallScheduleStatus.CANCELED);
        assertThat(response.status()).isEqualTo(TrainingCallScheduleStatus.CANCELED);
    }

    @Test
    void cancelRejectsAcceptedSchedule() {
        Users user = givenAuthenticatedUser();
        TrainingCallSchedule schedule = schedule(user);
        schedule.accept("session-id", LocalDateTime.now());
        when(trainingCallScheduleRepository.findByScheduleIdAndUser(1L, user)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> service.cancel(1L))
                .isInstanceOf(ApplicationException.class)
                .extracting(ex -> ((ApplicationException) ex).getStatusCode())
                .isEqualTo(TrainingCallScheduleStatusCode.SCHEDULE_NOT_CANCELABLE);
    }

    @Test
    void acceptCreatesSessionAndMarksScheduleAccepted() {
        Users user = givenAuthenticatedUser();
        TrainingCallSchedule schedule = schedule(user);
        schedule.markRinging(LocalDateTime.now());
        when(trainingCallScheduleRepository.findByScheduleIdAndUser(1L, user)).thenReturn(Optional.of(schedule));
        when(sessionService.create(any(CreateSessionRequest.class), eq("access-token")))
                .thenReturn(new CreateSessionResponse("session-id", "ws://voice"));

        AcceptTrainingCallScheduleResponse response = service.accept(1L, "access-token");

        ArgumentCaptor<CreateSessionRequest> captor = ArgumentCaptor.forClass(CreateSessionRequest.class);
        verify(sessionService).create(captor.capture(), eq("access-token"));
        CreateSessionRequest sessionRequest = captor.getValue();
        assertThat(sessionRequest.scenarioId()).isEqualTo(3L);
        assertThat(sessionRequest.type()).isEqualTo(SessionType.SCENARIO);
        assertThat(sessionRequest.aiPersonality()).isEqualTo(AiPersonality.NORMAL);
        assertThat(schedule.getStatus()).isEqualTo(TrainingCallScheduleStatus.ACCEPTED);
        assertThat(response.status()).isEqualTo(TrainingCallScheduleStatus.ACCEPTED);
        assertThat(response.sessionId()).isEqualTo("session-id");
        assertThat(response.wsUrl()).isEqualTo("ws://voice");
    }

    @Test
    void acceptRejectsScheduleThatIsNotRinging() {
        Users user = givenAuthenticatedUser();
        TrainingCallSchedule schedule = schedule(user);
        when(trainingCallScheduleRepository.findByScheduleIdAndUser(1L, user)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> service.accept(1L, "access-token"))
                .isInstanceOf(ApplicationException.class)
                .extracting(ex -> ((ApplicationException) ex).getStatusCode())
                .isEqualTo(TrainingCallScheduleStatusCode.SCHEDULE_NOT_ACCEPTABLE);
        verify(sessionService, never()).create(any(), any());
    }

    private Users givenAuthenticatedUser() {
        Users user = mock(Users.class);
        when(user.getUserId()).thenReturn(7L);
        when(usersRepository.findByUsername("junha")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("junha", null)
        );
        return user;
    }

    private TrainingCallSchedule schedule(Users user) {
        return TrainingCallSchedule.builder()
                .user(user)
                .scenarioId(3L)
                .type(SessionType.SCENARIO)
                .aiPersonality(AiPersonality.NORMAL)
                .minDelayMinutes(0)
                .maxDelayMinutes(180)
                .scheduledAt(LocalDateTime.now().plusMinutes(30))
                .build();
    }
}
