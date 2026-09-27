package ChickenMayoDeopbab.bada.domain.user.controller;

import ChickenMayoDeopbab.bada.domain.user.dto.request.UserModerationStatusRequest;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.service.UserModerationService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
public class InternalUserModerationController {

    private final UserModerationService userModerationService;

    @Value("${app.internal.secret}")
    private String internalSecret;

    @PatchMapping("/{userId}/moderation-status")
    public ApiResponse<Void> updateModerationStatus(
            @PathVariable Long userId,
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @Valid @RequestBody UserModerationStatusRequest request) {
        if (!internalSecret.equals(secret)) {
            throw ApplicationException.of(UsersStatusCode.INVALID_INTERNAL_SECRET);
        }

        userModerationService.updateStatus(userId, request);
        return ApiResponse.ok("사용자 상태가 변경되었습니다.");
    }
}
