package ChickenMayoDeopbab.bada.domain.auth.exception;

import ChickenMayoDeopbab.bada.global.exception.statuscode.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum AuthStatusCode implements StatusCode {
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "Auth-001", "비밀번호가 일치하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Auth-002", "유효하지 않은 RefreshToken입니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "Auth-003", "인증 코드가 일치하지 않습니다."),
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "Auth-004", "변경하려는 비밀번호와 현재 비밀번호가 같습니다."),
    ALREADY_EXIST_USERNAME(HttpStatus.CONFLICT, "Auth-005", "이미 존재하는 아이디입니다."),
    INVALID_OAUTH_CODE(HttpStatus.UNAUTHORIZED, "Auth-006", "유효하지 않거나 만료된 소셜 로그인 코드입니다."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Auth-007", "인증이 필요합니다."),
    ;
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
