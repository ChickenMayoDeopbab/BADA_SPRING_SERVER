package ChickenMayoDeopbab.bada.domain.dashboard.controller;

import ChickenMayoDeopbab.bada.domain.dashboard.dto.response.DashboardMetricsResponse;
import ChickenMayoDeopbab.bada.domain.dashboard.dto.response.WeeklySummaryResponse;
import ChickenMayoDeopbab.bada.domain.dashboard.service.DashboardService;
import ChickenMayoDeopbab.bada.domain.dashboard.service.DashboardAnalyticsService;
import ChickenMayoDeopbab.bada.domain.dashboard.dto.request.DashboardQuery;
import ChickenMayoDeopbab.bada.domain.dashboard.dto.response.DashboardAnalyticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.ModelAttribute;
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
    private final DashboardAnalyticsService dashboardAnalyticsService;

    @GetMapping("/overview")
    @Operation(summary = "기간별 훈련 성장 개요 조회")
    public ApiResponse<DashboardAnalyticsResponse.Overview> overview(@ParameterObject @ModelAttribute DashboardQuery query) {
        return ApiResponse.ok(dashboardAnalyticsService.overview(query));
    }

    @GetMapping("/comparison")
    @Operation(summary = "전체 및 동일 조건의 두 기간 비교")
    public ApiResponse<DashboardAnalyticsResponse.Comparison> comparison(@ParameterObject @ModelAttribute DashboardQuery query) {
        return ApiResponse.ok(dashboardAnalyticsService.comparison(query));
    }

    @GetMapping("/scenarios")
    @Operation(summary = "상황별 훈련 현황과 변화 조회")
    public ApiResponse<DashboardAnalyticsResponse.Scenarios> scenarios(@ParameterObject @ModelAttribute DashboardQuery query) {
        return ApiResponse.ok(dashboardAnalyticsService.scenarios(query));
    }

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
