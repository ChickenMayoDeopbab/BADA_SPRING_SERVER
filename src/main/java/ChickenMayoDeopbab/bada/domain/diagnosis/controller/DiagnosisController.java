package ChickenMayoDeopbab.bada.domain.diagnosis.controller;

import ChickenMayoDeopbab.bada.domain.diagnosis.dto.request.DiagnosisSubmitRequest;
import ChickenMayoDeopbab.bada.domain.diagnosis.dto.response.DiagnosisQuestionResponse;
import ChickenMayoDeopbab.bada.domain.diagnosis.dto.response.DiagnosisResultResponse;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisType;
import ChickenMayoDeopbab.bada.domain.diagnosis.service.DiagnosisService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diagnosis")
@RequiredArgsConstructor
public class DiagnosisController {
    private final DiagnosisService diagnosisService;

    @GetMapping("/questions")
    public ApiResponse<List<DiagnosisQuestionResponse>> getQuestions(@RequestParam DiagnosisType type) {
        return ApiResponse.ok(diagnosisService.getQuestions(type));
    }

    @PostMapping("/submit")
    public ApiResponse<DiagnosisResultResponse> submitAnswers(@RequestBody DiagnosisSubmitRequest request) {
        return ApiResponse.ok(diagnosisService.submitAnswers(request));
    }
}
