package ChickenMayoDeopbab.bada.domain.notification.exception;

import ChickenMayoDeopbab.bada.global.exception.statuscode.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationStatusCode implements StatusCode {
    INVALID_INTERNAL_SECRET(HttpStatus.UNAUTHORIZED, "NOTIFICATION_001", "내부 인증에 실패했습니다."),
    RECIPIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_002", "알림 수신자를 찾을 수 없습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_003", "알림을 찾을 수 없습니다."),
    ACTOR_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_004", "알림 발신자를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
