package ChickenMayoDeopbab.bada.domain.auth.exception;

import ChickenMayoDeopbab.bada.global.exception.statuscode.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum AuthStatusCode implements StatusCode {
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "Auth-001", "비밀번호가 일치하지 않습니다.")
    ;
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
