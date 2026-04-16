package ChickenMayoDeopbab.bada.global.exception.statuscode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum JwtStatusCode implements StatusCode {
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "JWT_001", "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "JWT_002", "유효하지 않은 토큰입니다."),
    TOKEN_MALFORMED(HttpStatus.UNAUTHORIZED, "JWT_003", "잘못된 형식의 토큰입니다."),
    REFRESH_TOKEN_NOT_ALLOWED(HttpStatus.UNAUTHORIZED, "JWT_004", "Access Token을 사용해주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}