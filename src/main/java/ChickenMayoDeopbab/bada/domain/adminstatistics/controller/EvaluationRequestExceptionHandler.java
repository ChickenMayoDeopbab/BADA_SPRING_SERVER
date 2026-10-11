package ChickenMayoDeopbab.bada.domain.adminstatistics.controller;

import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.exception.statuscode.CommonStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AdminEvaluationController.class)
public class EvaluationRequestExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> invalid(HttpMessageNotReadableException exception) {
        return CommonStatusCode.INVALID_ARGUMENT.toEntity("평가 설정의 형식과 값 범위를 확인해 주세요.");
    }
}
