package ChickenMayoDeopbab.bada.domain.file.exception;

import ChickenMayoDeopbab.bada.global.exception.statuscode.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FileStatusCode implements StatusCode {
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "FILE_001", "업로드할 파일이 비어있습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_002", "파일 업로드에 실패했습니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_003", "파일을 찾을 수 없습니다."),
    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "FILE_004", "이미지 파일만 업로드할 수 있습니다."),
    INVALID_RINGTONE_TYPE(HttpStatus.BAD_REQUEST, "FILE_005", "유효한 MP3 또는 PCM WAV 음원만 업로드할 수 있습니다."),
    RINGTONE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_006", "벨소리 파일은 최대 10MB까지 업로드할 수 있습니다."),
    UPLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_007", "파일 업로드 크기 제한을 초과했습니다."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
