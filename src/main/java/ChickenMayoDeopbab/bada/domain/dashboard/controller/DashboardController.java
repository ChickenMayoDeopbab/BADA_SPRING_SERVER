package ChickenMayoDeopbab.bada.domain.dashboard.controller;

import ChickenMayoDeopbab.bada.domain.dashboard.dto.response.DashboardMetricsResponse;
import ChickenMayoDeopbab.bada.domain.dashboard.dto.response.WeeklySummaryResponse;
import ChickenMayoDeopbab.bada.domain.dashboard.service.DashboardService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/metrics")
    public ApiResponse<DashboardMetricsResponse> getMetrics() {
        return ApiResponse.ok(
                dashboardService.getWeeklyMetrics()
        );
    }

    @GetMapping("/weekly-summary")
    public ApiResponse<WeeklySummaryResponse>
    getWeeklySummary() {
        return ApiResponse.ok(
                dashboardService.getWeeklySummary()
        );
    }
}
