package ChickenMayoDeopbab.bada.global.common;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        Map<String, String> details,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null, LocalDateTime.now());
    }

    public static ErrorResponse of(String code, String message, Map<String, String> details) {
        return new ErrorResponse(code, message, details, LocalDateTime.now());
    }
}