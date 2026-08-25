package ChickenMayoDeopbab.bada.domain.trainingrecord.exception;

import ChickenMayoDeopbab.bada.global.exception.statuscode.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TrainingRecordStatusCode implements StatusCode {
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "TRAINING_RECORD_001", "훈련 기록을 찾을 수 없습니다."),
    ANXIETY_SCORE_ALREADY_RECORDED(HttpStatus.CONFLICT, "TRAINING_RECORD_002", "불안 점수가 이미 기록되었습니다."),
    ANXIETY_SCORE_NOT_ALLOWED(HttpStatus.CONFLICT, "TRAINING_RECORD_003", "해당 훈련에는 불안 점수를 기록할 수 없습니다."),
    CALL_ANXIETY_STATE_NOT_FOUND(HttpStatus.CONFLICT, "TRAINING_RECORD_004", "최초 자가진단 상태를 찾을 수 없습니다."),
    SCORE_APPLIED_RECORD_CANNOT_BE_DELETED(HttpStatus.CONFLICT, "TRAINING_RECORD_005", "점수에 반영된 훈련 기록은 삭제할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
