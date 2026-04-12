package ChickenMayoDeopbab.bada.global.common;

import org.springframework.http.HttpStatus;

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
        return of(HttpStatus.OK, message, data,null);
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return of(HttpStatus.CREATED, message, data, null);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, ErrorResponse error) {
        return of(status, error.message(), null, error);
    }
}