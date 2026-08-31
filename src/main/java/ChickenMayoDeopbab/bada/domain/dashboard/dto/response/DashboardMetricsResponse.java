package ChickenMayoDeopbab.bada.domain.dashboard.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardMetricsResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        List<LocalDate> dates,
        List<BigDecimal> stabilityScores,
        List<BigDecimal> conversationScores,
        List<BigDecimal> fluencyScores,
        CallPhobiaLevelResponse callPhobiaLevel
) {
}
