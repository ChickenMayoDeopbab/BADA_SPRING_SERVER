package ChickenMayoDeopbab.bada.domain.notification.controller;

import ChickenMayoDeopbab.bada.domain.notification.dto.response.InAppNotificationListResponse;
import ChickenMayoDeopbab.bada.domain.notification.dto.response.InAppNotificationResponse;
import ChickenMayoDeopbab.bada.domain.notification.model.NotificationFilter;
import ChickenMayoDeopbab.bada.domain.notification.service.InAppNotificationService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class InAppNotificationController {

    private final InAppNotificationService inAppNotificationService;

    @GetMapping
    public ApiResponse<InAppNotificationListResponse> getNotifications(
            @RequestParam(defaultValue = "ALL") NotificationFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(
                inAppNotificationService.getNotifications(filter, PageRequest.of(page, size))
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<InAppNotificationResponse> markRead(
            @PathVariable Long notificationId
    ) {
        return ApiResponse.ok(
                inAppNotificationService.markRead(notificationId),
                "알림을 읽음 처리했습니다."
        );
    }
}
