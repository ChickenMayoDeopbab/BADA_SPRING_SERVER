package ChickenMayoDeopbab.bada.domain.file.controller;

import ChickenMayoDeopbab.bada.domain.file.dto.response.GetUrlResponse;
import ChickenMayoDeopbab.bada.domain.file.service.FileService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/file")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @GetMapping
    public ApiResponse<GetUrlResponse> getPresignedUrl(@RequestParam("fileId") Long fileId) {
        String url = fileService.getUrl(fileId);
        return ApiResponse.ok(new GetUrlResponse(url));
    }
}
