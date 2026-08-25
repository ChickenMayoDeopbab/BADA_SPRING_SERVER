package ChickenMayoDeopbab.bada.domain.session.controller;

import ChickenMayoDeopbab.bada.domain.session.dto.request.SessionClosedRequest;
import ChickenMayoDeopbab.bada.domain.session.dto.response.SessionClosedResponse;
import ChickenMayoDeopbab.bada.domain.session.exception.SessionStatusCode;
import ChickenMayoDeopbab.bada.domain.session.service.SessionService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

// FastAPI 내부 호출 전용
@RestController
@RequestMapping("/internal/v1/sessions")
@RequiredArgsConstructor
public class InternalSessionController {

    private final SessionService sessionService;

    @Value("${app.internal.secret}")
    private String internalSecret;

    @PostMapping("/{sessionId}/closed")
    public ApiResponse<SessionClosedResponse> onSessionClosed(
            @PathVariable String sessionId,
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @RequestBody SessionClosedRequest request) {
        if (!internalSecret.equals(secret)) {
            throw ApplicationException.of(SessionStatusCode.INVALID_INTERNAL_SECRET);
        }
        sessionService.close(
                sessionId,
                request.reason(),
                request.transcript(),
                request.silenceTotal(),
                request.shakeCount(),
                request.goodSegments(),
                request.recordingKey(),
                request.analysis()
        );
        return ApiResponse.ok("세션 종료가 처리되었습니다.");
    }
}
