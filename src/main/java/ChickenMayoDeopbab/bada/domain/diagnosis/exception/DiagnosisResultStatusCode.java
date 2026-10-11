package ChickenMayoDeopbab.bada.domain.diagnosis.exception;

import ChickenMayoDeopbab.bada.global.exception.statuscode.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DiagnosisResultStatusCode implements StatusCode {
    DIAGNOSIS_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "DIAGNOSIS_RESULT_NOT_FOUND", "진단 결과를 찾을 수 없습니다."),
    INVALID_DIAGNOSIS_ANSWERS(HttpStatus.BAD_REQUEST, "INVALID_DIAGNOSIS_ANSWERS", "자가진단 문항과 응답이 일치하지 않습니다."),
    DIAGNOSIS_SESSION_CONFLICT(HttpStatus.CONFLICT, "DIAGNOSIS_SESSION_CONFLICT", "이미 다른 응답에 사용된 검사 ID입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
