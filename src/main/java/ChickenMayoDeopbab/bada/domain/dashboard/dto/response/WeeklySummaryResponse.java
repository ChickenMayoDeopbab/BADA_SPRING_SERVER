package ChickenMayoDeopbab.bada.domain.dashboard.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklySummaryResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        long trainingCount,
        long totalCallDurationSeconds,
        BigDecimal averageStabilityScore,
        BigDecimal averageConversationScore,
        BigDecimal averageFluencyScore,
        String comment
) {
}
