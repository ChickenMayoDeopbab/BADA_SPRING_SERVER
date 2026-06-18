package ChickenMayoDeopbab.bada.domain.session.service;

import ChickenMayoDeopbab.bada.domain.session.dto.request.CreateSessionRequest;
import ChickenMayoDeopbab.bada.domain.session.dto.response.CreateSessionResponse;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.session.model.ScenarioContext;
import ChickenMayoDeopbab.bada.domain.session.model.SessionContext;
import ChickenMayoDeopbab.bada.domain.session.port.ScenarioPort;
import ChickenMayoDeopbab.bada.domain.session.repository.SessionRedisRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final UsersRepository usersRepository;
    private final ScenarioPort scenarioPort;
    private final SessionRedisRepository sessionRedisRepository;

    @Value("${app.ai.ws-base-url}")
    private String wsBaseUrl;

    public CreateSessionResponse create(CreateSessionRequest request, String accessToken) {
        Users user = getUserInfo();

        ScenarioContext scenario = scenarioPort.fetch(request.scenarioId(), request.type());

        SessionContext context = new SessionContext(
                user.getUserId(),
                request.type(),
                request.aiPersonality(),
                resolveMaxDuration(request),
                scenario
        );

        String sessionId = UUID.randomUUID().toString();
        sessionRedisRepository.save(sessionId, context);

        String wsUrl = wsBaseUrl + "/ws/voice/" + sessionId + "?token=" + accessToken;
        return new CreateSessionResponse(sessionId, wsUrl);
    }

    private Integer resolveMaxDuration(CreateSessionRequest request) {
        if (request.maxDurationSeconds() != null) return request.maxDurationSeconds();
        return request.type() == SessionType.WARMUP ? 30 : null;
    }

    private Users getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return usersRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));
    }
}