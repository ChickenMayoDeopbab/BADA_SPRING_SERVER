package ChickenMayoDeopbab.bada.domain.trainingrecord.controller;

import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request.RecordAnxietyScoreRequest;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.AnxietyScoreResponse;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.FeedbackResponse;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.TrainingRecordDetailResponse;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.TrainingRecordResponse;
import ChickenMayoDeopbab.bada.domain.trainingrecord.service.TrainingRecordService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/training-records")
@RequiredArgsConstructor
public class TrainingRecordController {

    private final TrainingRecordService trainingRecordService;
    @PostMapping("/{sessionId}/anxiety-score")
    public ApiResponse<AnxietyScoreResponse> recordAnxietyScore(@PathVariable String sessionId,
                                                                @Valid @RequestBody RecordAnxietyScoreRequest request) {
        return ApiResponse.created(trainingRecordService.recordAnxietyScore(sessionId, request.score()), "불안 점수가 기록되었습니다.");
    }

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

    @GetMapping("/feedback")
    public FeedbackResponse getFeedback(@RequestParam("scenarioId") Long scenarioId) {
        return trainingRecordService.getFeedback(scenarioId);
    }

    @DeleteMapping("/{recordId}")
    public ApiResponse<Void> deleteTrainingRecord(@PathVariable Long recordId) {
        trainingRecordService.deleteTrainingRecord(recordId);
        return ApiResponse.ok("훈련 기록이 삭제되었습니다.");
    }
}
