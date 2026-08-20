package ChickenMayoDeopbab.bada.domain.file.controller;

import ChickenMayoDeopbab.bada.domain.file.dto.response.FileUploadResponse;
import ChickenMayoDeopbab.bada.domain.file.dto.response.GetUrlResponse;
import ChickenMayoDeopbab.bada.domain.file.enumeration.FileType;
import ChickenMayoDeopbab.bada.domain.file.service.FileService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping
    public ApiResponse<FileUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") FileType fileType) {
        return ApiResponse.created(fileService.upload(file, fileType), "파일 업로드 성공");
    }
}
