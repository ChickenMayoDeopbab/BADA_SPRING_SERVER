package ChickenMayoDeopbab.bada.global.exception.statuscode;

import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface StatusCode {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();

    default ResponseEntity<ApiResponse<Void>> toEntity() {
        ErrorResponse error = ErrorResponse.of(getCode(), getMessage());
        return ResponseEntity
                .status(getHttpStatus())
                .body(ApiResponse.error(getHttpStatus(), error));
    }

    default ResponseEntity<ApiResponse<Void>> toEntity(String message) {
        ErrorResponse error = ErrorResponse.of(getCode(), message);
        return ResponseEntity
                .status(getHttpStatus())
                .body(ApiResponse.error(getHttpStatus(), error));
    }

    default ResponseEntity<ApiResponse<Void>> toEntity(String message, Map<String, String> details) {
        ErrorResponse error = ErrorResponse.of(getCode(), message, details);
        return ResponseEntity
                .status(getHttpStatus())
                .body(ApiResponse.error(getHttpStatus(), error));
    }
}