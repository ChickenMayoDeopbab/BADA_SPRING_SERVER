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
    INVALID_INTERNAL_SECRET(HttpStatus.UNAUTHORIZED, "USER_005", "내부 인증에 실패했습니다."),
    INVALID_MODERATION_STATUS(HttpStatus.BAD_REQUEST, "USER_006", "사용자 제재 요청이 유효하지 않습니다."),
    MODERATOR_NOT_ALLOWED(HttpStatus.FORBIDDEN, "USER_007", "사용자 제재 권한이 없습니다."),
    CANNOT_SANCTION_ADMIN(HttpStatus.FORBIDDEN, "USER_008", "관리자 계정은 제재할 수 없습니다."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "USER_009", "일시 정지된 계정입니다."),
    USER_BANNED(HttpStatus.FORBIDDEN, "USER_010", "영구 정지된 계정입니다."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
