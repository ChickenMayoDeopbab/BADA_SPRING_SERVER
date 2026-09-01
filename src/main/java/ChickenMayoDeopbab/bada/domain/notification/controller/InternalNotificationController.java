package ChickenMayoDeopbab.bada.domain.notification.controller;

import ChickenMayoDeopbab.bada.domain.notification.dto.request.CommunityNotificationRequest;
import ChickenMayoDeopbab.bada.domain.notification.exception.NotificationStatusCode;
import ChickenMayoDeopbab.bada.domain.notification.service.CommunityNotificationService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final CommunityNotificationService communityNotificationService;

    @Value("${app.internal.secret}")
    private String internalSecret;

    @PostMapping("/community")
    public ApiResponse<Void> sendCommunityNotification(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @Valid @RequestBody CommunityNotificationRequest request
    ) {
        if (!internalSecret.equals(secret)) {
            throw ApplicationException.of(NotificationStatusCode.INVALID_INTERNAL_SECRET);
        }

        communityNotificationService.send(request);
        return ApiResponse.ok("커뮤니티 알림이 처리되었습니다.");
    }
}
