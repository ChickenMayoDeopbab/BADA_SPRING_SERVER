package ChickenMayoDeopbab.bada.domain.adminstatistics.model;

import java.time.LocalDateTime;

public record SelfAssessmentStatisticsRow(
        Long resultId,
        Long userId,
        double score,
        LocalDateTime assessedAt
) {
}
