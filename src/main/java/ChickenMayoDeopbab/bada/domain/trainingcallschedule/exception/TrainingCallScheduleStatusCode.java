package ChickenMayoDeopbab.bada.domain.trainingcallschedule.exception;

import ChickenMayoDeopbab.bada.global.exception.statuscode.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TrainingCallScheduleStatusCode implements StatusCode {
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "TRAINING_CALL_SCHEDULE_001", "예약된 발신을 찾을 수 없습니다."),
    INVALID_DELAY_RANGE(HttpStatus.BAD_REQUEST, "TRAINING_CALL_SCHEDULE_002", "발신 시간 범위가 올바르지 않습니다."),
    SCHEDULE_NOT_CANCELABLE(HttpStatus.BAD_REQUEST, "TRAINING_CALL_SCHEDULE_003", "취소할 수 없는 발신 예약입니다."),
    SCHEDULE_NOT_ACCEPTABLE(HttpStatus.BAD_REQUEST, "TRAINING_CALL_SCHEDULE_004", "수신할 수 없는 발신 예약입니다."),
    SCHEDULE_FORBIDDEN(HttpStatus.FORBIDDEN, "TRAINING_CALL_SCHEDULE_005", "해당 발신 예약에 접근할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
