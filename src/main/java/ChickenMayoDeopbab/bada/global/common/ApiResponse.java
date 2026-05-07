package ChickenMayoDeopbab.bada.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        int status,
        String message,
        T data,
        ErrorResponse error
) {
    public static <T> ApiResponse<T> of(HttpStatus status, String message, T data, ErrorResponse error) {
        return new ApiResponse<>(status.value(), message, data, error);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return of(HttpStatus.OK, message, data, null);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return of(HttpStatus.OK, null, data, null);
    }

    public static <T> ApiResponse<T> ok(String message) {
        return of(HttpStatus.OK, message, null, null);
    }

    public static <T> ApiResponse<T> ok() {
        return of(HttpStatus.OK, null, null, null);
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return of(HttpStatus.CREATED, message, data, null);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, ErrorResponse error) {
        return of(status, error.message(), null, error);
    }
}