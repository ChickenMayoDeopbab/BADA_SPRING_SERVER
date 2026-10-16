package ChickenMayoDeopbab.bada.domain.dashboard.exception;

import ChickenMayoDeopbab.bada.global.exception.statuscode.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DashboardStatusCode implements StatusCode {
    INVALID_QUERY(HttpStatus.BAD_REQUEST, "DASHBOARD_001", "대시보드 조회 조건이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
