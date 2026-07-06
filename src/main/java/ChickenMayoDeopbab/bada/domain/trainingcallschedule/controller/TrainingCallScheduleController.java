package ChickenMayoDeopbab.bada.domain.trainingcallschedule.controller;

import ChickenMayoDeopbab.bada.domain.trainingcallschedule.dto.request.CreateTrainingCallScheduleRequest;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.dto.response.AcceptTrainingCallScheduleResponse;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.dto.response.TrainingCallScheduleResponse;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.service.TrainingCallScheduleService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/training-call-schedules")
@RequiredArgsConstructor
public class TrainingCallScheduleController {

    private final TrainingCallScheduleService trainingCallScheduleService;
    private final JwtProvider jwtProvider;

    @PostMapping
    public ApiResponse<TrainingCallScheduleResponse> create(
            @Valid @RequestBody CreateTrainingCallScheduleRequest request
    ) {
        return ApiResponse.created(
                trainingCallScheduleService.create(request),
                "AI 발신 예약이 생성되었습니다."
        );
    }

    @DeleteMapping("/{scheduleId}")
    public ApiResponse<TrainingCallScheduleResponse> cancel(@PathVariable Long scheduleId) {
        return ApiResponse.ok(
                trainingCallScheduleService.cancel(scheduleId),
                "AI 발신 예약이 취소되었습니다."
        );
    }

    @PostMapping("/{scheduleId}/accept")
    public ApiResponse<AcceptTrainingCallScheduleResponse> accept(
            @PathVariable Long scheduleId,
            HttpServletRequest httpRequest
    ) {
        String accessToken = jwtProvider.resolveToken(httpRequest);
        return ApiResponse.ok(
                trainingCallScheduleService.accept(scheduleId, accessToken),
                "AI 발신을 수신했습니다."
        );
    }
}
