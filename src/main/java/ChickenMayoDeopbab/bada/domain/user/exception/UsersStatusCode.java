package ChickenMayoDeopbab.bada.domain.user.exception;

import ChickenMayoDeopbab.bada.global.exception.statuscode.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UsersStatusCode implements StatusCode {
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "USER_001", "중복된 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER_002", "중복된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_003", "사용자를 찾을 수 없습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "USER_004", "이메일 인증이 되지 않았습니다."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
