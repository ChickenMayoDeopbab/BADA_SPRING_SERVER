package ChickenMayoDeopbab.bada.domain.adminstatistics.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CallAnxietySummaryResponse(
        OffsetDateTime generatedAt,
        String scope,
        String scoringVersion,
        long totalUserCount,
        long eligibleUserCount,
        long improvedUserCount,
        BigDecimal improvementRate,
        BigDecimal averageInitialScore,
        BigDecimal averageCurrentIndex,
        BigDecimal averageScoreChange,
        long levelImprovedUserCount,
        BigDecimal levelImprovementRate
) {
}
