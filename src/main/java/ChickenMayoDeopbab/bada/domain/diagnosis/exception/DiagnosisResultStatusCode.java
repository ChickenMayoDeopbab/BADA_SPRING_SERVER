package ChickenMayoDeopbab.bada.domain.diagnosis.exception;

import ChickenMayoDeopbab.bada.global.exception.statuscode.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DiagnosisResultStatusCode implements StatusCode {
    DIAGNOSIS_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "DIAGNOSIS_RESULT_NOT_FOUND", "진단 결과를 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
