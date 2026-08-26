package ChickenMayoDeopbab.bada.domain.adminstatistics.model;

import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;

import java.math.BigDecimal;

public record AppliedTrainingStatisticsRow(
        Long userId,
        String difficulty,
        AiPersonality personality,
        String analyzerVersion,
        String scoringVersion,
        BigDecimal trainingStateIndex,
        Long scoreSequence
) {
}
