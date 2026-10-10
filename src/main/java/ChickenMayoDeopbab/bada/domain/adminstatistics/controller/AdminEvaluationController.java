package ChickenMayoDeopbab.bada.domain.adminstatistics.controller;

import ChickenMayoDeopbab.bada.domain.adminstatistics.dto.request.EvaluationRequest;
import ChickenMayoDeopbab.bada.domain.adminstatistics.service.AdminEvaluationService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/admin/evaluations")
@RequiredArgsConstructor
public class AdminEvaluationController {
    private final AdminEvaluationService service;
    @GetMapping("/accounts")
    public ApiResponse<Page<AdminEvaluationService.Account>> accounts(@RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.ok(service.accounts(search, page, size));
    }
    @GetMapping("/cohorts") public ApiResponse<List<AdminEvaluationService.Cohort>> cohorts() { return ApiResponse.ok(service.cohorts()); }
    @GetMapping("/accounts/{id}/assessments")
    public ApiResponse<Page<AdminEvaluationService.Assessment>> assessments(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.assessments(id, page, size));
    }
    @PostMapping("/cohorts")
    public ApiResponse<AdminEvaluationService.Cohort> create(@Valid @RequestBody EvaluationRequest.CohortRequest request) {
        return ApiResponse.ok(service.createCohort(request.name()));
    }
    @PutMapping("/cohorts/{id}/participants")
    public ApiResponse<AdminEvaluationService.Cohort> members(@PathVariable Long id, @Valid @RequestBody EvaluationRequest.MembersRequest request) {
        return ApiResponse.ok(service.replaceMembers(id, request.members()));
    }
    @PostMapping("/runs") public ApiResponse<AdminEvaluationService.Run> execute(@Valid @RequestBody EvaluationRequest request) {
        return ApiResponse.ok(service.execute(request));
    }
    @GetMapping("/runs") public ApiResponse<Page<AdminEvaluationService.RunHeader>> runs(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.runs(page, size));
    }
    @GetMapping("/runs/{id}") public ApiResponse<AdminEvaluationService.Run> run(@PathVariable Long id) { return ApiResponse.ok(service.run(id)); }
    @GetMapping(value = "/runs/{id}/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        var export = service.export(id);
        return ResponseEntity.ok().contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(export.fileName()).build().toString())
                .body(export.content());
    }
}
