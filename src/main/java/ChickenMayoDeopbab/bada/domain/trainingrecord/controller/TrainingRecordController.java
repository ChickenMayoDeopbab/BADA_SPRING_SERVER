package ChickenMayoDeopbab.bada.domain.trainingrecord.controller;

import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.TrainingRecordDetailResponse;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.TrainingRecordResponse;
import ChickenMayoDeopbab.bada.domain.trainingrecord.service.TrainingRecordService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/training-records")
@RequiredArgsConstructor
public class TrainingRecordController {

    private final TrainingRecordService trainingRecordService;

    @GetMapping
    public ApiResponse<Page<TrainingRecordResponse>> getTrainingRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(trainingRecordService.getTrainingRecords(PageRequest.of(page, size)));
    }

    @GetMapping("/{recordId}")
    public ApiResponse<TrainingRecordDetailResponse> getTrainingRecord(@PathVariable Long recordId) {
        return ApiResponse.ok(trainingRecordService.getTrainingRecord(recordId));
    }
}
