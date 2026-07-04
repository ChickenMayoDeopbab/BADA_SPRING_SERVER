package ChickenMayoDeopbab.bada.domain.session.service;

import ChickenMayoDeopbab.bada.domain.session.dto.request.CreateSessionRequest;
import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.session.model.SessionContext;
import ChickenMayoDeopbab.bada.domain.session.port.ScenarioPort;
import ChickenMayoDeopbab.bada.domain.session.port.SessionRecordPort;
import ChickenMayoDeopbab.bada.domain.session.repository.SessionRedisRepository;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolveScriptLevelBoundaries() {
        assertThat(SessionService.resolveScriptLevel(0)).isEqualTo(1);
        assertThat(SessionService.resolveScriptLevel(5)).isEqualTo(1);
        assertThat(SessionService.resolveScriptLevel(6)).isEqualTo(2);
        assertThat(SessionService.resolveScriptLevel(10)).isEqualTo(2);
        assertThat(SessionService.resolveScriptLevel(11)).isEqualTo(3);
        assertThat(SessionService.resolveScriptLevel(100)).isEqualTo(3);
    }

    @Test
    void createInjectsScriptLevelFromTrainingCount() {
        UsersRepository usersRepository = mock(UsersRepository.class);
        ScenarioPort scenarioPort = mock(ScenarioPort.class);
        SessionRedisRepository sessionRedisRepository = mock(SessionRedisRepository.class);
        SessionRecordPort sessionRecordPort = mock(SessionRecordPort.class);
        TrainingRecordRepository trainingRecordRepository = mock(TrainingRecordRepository.class);

        Users user = mock(Users.class);
        when(user.getUserId()).thenReturn(7L);
        when(usersRepository.findByUsername("junha")).thenReturn(Optional.of(user));
        when(trainingRecordRepository.countByUserAndScenarioIdAndEndReasonNotIn(
                eq(user), eq(3L), anyCollection())).thenReturn(7L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("junha", null));

        SessionService service = new SessionService(
                usersRepository,
                scenarioPort,
                sessionRedisRepository,
                sessionRecordPort,
                trainingRecordRepository
        );

        service.create(
                new CreateSessionRequest(3L, SessionType.SCENARIO, AiPersonality.NORMAL, null, 180),
                "access-token"
        );

        ArgumentCaptor<SessionContext> captor = ArgumentCaptor.forClass(SessionContext.class);
        verify(sessionRedisRepository).save(anyString(), captor.capture());
        assertThat(captor.getValue().scriptLevel()).isEqualTo(2);
        assertThat(captor.getValue().userId()).isEqualTo(7L);
    }
}
