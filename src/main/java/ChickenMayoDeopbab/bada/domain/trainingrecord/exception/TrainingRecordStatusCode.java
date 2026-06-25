package ChickenMayoDeopbab.bada.domain.trainingrecord.exception;

import ChickenMayoDeopbab.bada.global.exception.statuscode.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TrainingRecordStatusCode implements StatusCode {
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "TRAINING_RECORD_001", "훈련 기록을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
