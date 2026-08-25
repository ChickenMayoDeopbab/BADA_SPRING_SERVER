package ChickenMayoDeopbab.bada.domain.adminstatistics.controller;

import ChickenMayoDeopbab.bada.domain.adminstatistics.dto.response.CallAnxietySummaryResponse;
import ChickenMayoDeopbab.bada.domain.adminstatistics.model.CallAnxietyCsvExport;
import ChickenMayoDeopbab.bada.domain.adminstatistics.service.AdminCallAnxietyStatisticsService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/admin/statistics/call-anxiety")
@RequiredArgsConstructor
public class AdminCallAnxietyStatisticsController {
    private final AdminCallAnxietyStatisticsService adminCallAnxietyStatisticsService;

    @GetMapping("/summary")
    public ApiResponse<CallAnxietySummaryResponse> getSummary() {
        return ApiResponse.ok(
                adminCallAnxietyStatisticsService.getSummary()
        );
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv() {
        CallAnxietyCsvExport export =
                adminCallAnxietyStatisticsService.exportCsv();

        String contentDisposition =
                ContentDisposition.attachment()
                        .filename(
                                export.fileName(),
                                StandardCharsets.UTF_8
                        )
                        .build()
                        .toString();

        MediaType csvMediaType =
                new MediaType(
                        "text",
                        "csv",
                        StandardCharsets.UTF_8
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition
                )
                .contentType(csvMediaType)
                .contentLength(export.content().length)
                .body(export.content());
    }
}
