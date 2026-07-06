package ChickenMayoDeopbab.bada.domain.trainingcallschedule.controller;

import ChickenMayoDeopbab.bada.domain.trainingcallschedule.dto.request.CreateTrainingCallScheduleRequest;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.dto.response.TrainingCallScheduleResponse;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.service.TrainingCallScheduleService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
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
}
