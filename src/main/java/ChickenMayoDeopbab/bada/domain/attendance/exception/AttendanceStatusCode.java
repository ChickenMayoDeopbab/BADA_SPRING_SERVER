package ChickenMayoDeopbab.bada.domain.attendance.exception;

import ChickenMayoDeopbab.bada.global.exception.statuscode.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AttendanceStatusCode implements StatusCode {
    ALREADY_ATTENDED(HttpStatus.CONFLICT, "Attendance-001", "이미 출석을 한 계정입니다.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
