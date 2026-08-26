package ChickenMayoDeopbab.bada.domain.adminstatistics.model;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;

import java.math.BigDecimal;

public record CallAnxietyStateStatisticsRow(
        Long userId,
        BigDecimal initialSelfReportScore,
        BigDecimal currentCallAnxietyIndex,
        CallPhobiaLevel initialLevel,
        CallPhobiaLevel currentLevel,
        int validTrainingCount,
        String scoringVersion
) {
}
