package ChickenMayoDeopbab.bada.domain.notification.controller;

import ChickenMayoDeopbab.bada.domain.notification.dto.request.RegisterPushDeviceRequest;
import ChickenMayoDeopbab.bada.domain.notification.service.PushDeviceService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/devices")
@RequiredArgsConstructor
public class PushDeviceController {

    private final PushDeviceService pushDeviceService;

    @PutMapping
    public ApiResponse<Void> register(@Valid @RequestBody RegisterPushDeviceRequest request) {
        pushDeviceService.register(request);
        return ApiResponse.ok("푸시 알림 기기가 등록되었습니다.");
    }

    @DeleteMapping("/{installationId}")
    public ApiResponse<Void> unregister(@PathVariable String installationId) {
        pushDeviceService.unregister(installationId);
        return ApiResponse.ok("푸시 알림 기기가 해제되었습니다.");
    }
}
