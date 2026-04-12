package ChickenMayoDeopbab.bada.global.exception.statuscode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommonStatusCode implements StatusCode {
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 요청 파라미터입니다."),
    UNKNOWN_ENDPOINT(HttpStatus.NOT_FOUND, "COMMON_002", "존재하지 않는 API입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_003", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}