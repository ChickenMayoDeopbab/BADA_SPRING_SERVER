package ChickenMayoDeopbab.bada.domain.session.exception;

import ChickenMayoDeopbab.bada.global.exception.statuscode.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SessionStatusCode implements StatusCode {
    SCENARIO_NOT_FOUND(HttpStatus.NOT_FOUND, "SESSION_001", "시나리오를 찾을 수 없습니다."),
    REDIS_WRITE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SESSION_002", "세션 저장에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
