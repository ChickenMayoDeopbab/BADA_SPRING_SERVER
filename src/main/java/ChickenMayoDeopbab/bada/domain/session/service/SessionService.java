package ChickenMayoDeopbab.bada.domain.session.service;

import ChickenMayoDeopbab.bada.domain.session.dto.request.CreateSessionRequest;
import ChickenMayoDeopbab.bada.domain.session.dto.response.CreateSessionResponse;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;
import ChickenMayoDeopbab.bada.domain.session.model.ScenarioContext;
import ChickenMayoDeopbab.bada.domain.session.model.SessionContext;
import ChickenMayoDeopbab.bada.domain.session.model.TranscriptTurn;
import ChickenMayoDeopbab.bada.domain.session.port.ScenarioPort;
import ChickenMayoDeopbab.bada.domain.session.port.SessionRecordPort;
import ChickenMayoDeopbab.bada.domain.session.repository.SessionRedisRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final UsersRepository usersRepository;
    private final ScenarioPort scenarioPort;
    private final SessionRedisRepository sessionRedisRepository;
    private final SessionRecordPort sessionRecordPort;

    @Value("${app.ai.ws-base-url}")
    private String wsBaseUrl;

    public CreateSessionResponse create(CreateSessionRequest request, String accessToken) {
        Users user = getUserInfo();

        ScenarioContext scenario = scenarioPort.fetch(request.scenarioId(), request.type());

        SessionContext context = new SessionContext(
                user.getUserId(),
                request.scenarioId(),
                request.type(),
                request.aiPersonality(),
                resolveMaxDuration(request),
                LocalDateTime.now(),
                scenario
        );

        String sessionId = UUID.randomUUID().toString();
        sessionRedisRepository.save(sessionId, context);

        String wsUrl = wsBaseUrl + "/ws/voice/" + sessionId + "?token=" + accessToken;
        return new CreateSessionResponse(sessionId, wsUrl);
    }

    // FastAPI 종료 콜백 처리, 기록 위임 후 레디스 세션 정리.
    public void close(
            String sessionId,
            EndReason reason,
            List<TranscriptTurn> transcript,
            Double silenceTotal,
            Integer shakeCount,
            List<GoodSegment> goodSegments
    ) {
        SessionContext context = sessionRedisRepository.find(sessionId);
        if (context == null) {
            log.warn("종료 콜백 - 세션 없음(만료/미존재) sessionId={} reason={}", sessionId, reason);
            return;
        }
        sessionRecordPort.record(sessionId, context, reason, transcript, silenceTotal, shakeCount, goodSegments);
        sessionRedisRepository.delete(sessionId);
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
