package ChickenMayoDeopbab.bada.domain.session.controller;

import ChickenMayoDeopbab.bada.domain.session.dto.request.CreateSessionRequest;
import ChickenMayoDeopbab.bada.domain.session.dto.response.CreateSessionResponse;
import ChickenMayoDeopbab.bada.domain.session.service.SessionService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ApiResponse<CreateSessionResponse> create(@RequestBody CreateSessionRequest request) {
        return ApiResponse.created(sessionService.create(request), "세션이 생성되었습니다.");
    }
}
