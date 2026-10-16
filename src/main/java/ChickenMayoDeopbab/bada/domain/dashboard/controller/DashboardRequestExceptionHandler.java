package ChickenMayoDeopbab.bada.domain.dashboard.controller;

import ChickenMayoDeopbab.bada.domain.dashboard.exception.DashboardStatusCode;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = DashboardController.class)
public class DashboardRequestExceptionHandler {
    @ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> invalidQuery(Exception exception) {
        return DashboardStatusCode.INVALID_QUERY.toEntity();
    }
}
