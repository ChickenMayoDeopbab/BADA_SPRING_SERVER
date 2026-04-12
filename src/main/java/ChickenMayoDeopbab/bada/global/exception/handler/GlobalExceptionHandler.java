package ChickenMayoDeopbab.bada.global.exception.handler;

import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import ChickenMayoDeopbab.bada.global.exception.statuscode.CommonStatusCode;
import ChickenMayoDeopbab.bada.global.exception.statuscode.StatusCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiResponse<Void>> handleApplicationException(ApplicationException ex) {
        StatusCode statusCode = ex.getStatusCode();
        String message = ex.getMessage() != null ? ex.getMessage() : statusCode.getMessage();
        return statusCode.toEntity(message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage() != null ? error.getDefaultMessage() : "잘못된 입력값 입니다.";
            details.put(field, message);
        });
        return CommonStatusCode.INVALID_ARGUMENT.toEntity("요청값이 유효하지 않습니다.", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> details = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            details.put(fieldName, message);
        });
        return CommonStatusCode.INVALID_ARGUMENT.toEntity("요청값이 유효하지 않습니다.", details);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        return CommonStatusCode.INVALID_ARGUMENT.toEntity("필수 파라미터가 누락되었습니다: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        return CommonStatusCode.INVALID_ARGUMENT.toEntity("파라미터 타입이 잘못되었습니다: " + ex.getName());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "잘못된 요청 파라미터입니다.";
        return CommonStatusCode.INVALID_ARGUMENT.toEntity(message);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException ex) {
        return CommonStatusCode.UNKNOWN_ENDPOINT.toEntity();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("요청 처리 중 에러 발생: {}", ex.getMessage());
        return CommonStatusCode.INTERNAL_SERVER_ERROR.toEntity();
    }
}