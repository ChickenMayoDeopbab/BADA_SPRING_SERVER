package ChickenMayoDeopbab.bada.domain.file.dto.response;

import ChickenMayoDeopbab.bada.domain.file.entity.File;

public record FileUploadResponse(
        Long fileId,
        String title,
        String s3Key,
        String url
) {
    public static FileUploadResponse of(File file, String url) {
        return new FileUploadResponse(
                file.getId(),
                file.getTitle(),
                file.getS3Key(),
                url
        );
    }
}