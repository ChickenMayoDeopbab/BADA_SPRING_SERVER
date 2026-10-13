package ChickenMayoDeopbab.bada.domain.ringtone.dto.response;

import ChickenMayoDeopbab.bada.domain.file.dto.response.FileUploadResponse;
import ChickenMayoDeopbab.bada.domain.file.entity.File;

public record RingtoneResponse(Long fileId, String title, String url) {

    public static RingtoneResponse from(File file, String url) {
        return new RingtoneResponse(file.getId(), file.getTitle(), url);
    }

    public static RingtoneResponse from(FileUploadResponse file) {
        return new RingtoneResponse(file.fileId(), file.title(), file.url());
    }
}
