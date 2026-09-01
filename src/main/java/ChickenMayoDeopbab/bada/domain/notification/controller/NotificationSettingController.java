package ChickenMayoDeopbab.bada.domain.notification.controller;

import ChickenMayoDeopbab.bada.domain.notification.dto.request.UpdateNotificationSettingRequest;
import ChickenMayoDeopbab.bada.domain.notification.dto.response.NotificationSettingResponse;
import ChickenMayoDeopbab.bada.domain.notification.service.NotificationSettingService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/settings")
@RequiredArgsConstructor
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;

    @GetMapping
    public ApiResponse<NotificationSettingResponse> get() {
        return ApiResponse.ok(notificationSettingService.get());
    }

    @PutMapping
    public ApiResponse<NotificationSettingResponse> update(
            @Valid @RequestBody UpdateNotificationSettingRequest request
    ) {
        return ApiResponse.ok(
                notificationSettingService.update(request),
                "알림 설정이 저장되었습니다."
        );
    }
}
