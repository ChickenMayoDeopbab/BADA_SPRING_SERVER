package ChickenMayoDeopbab.bada.domain.callanxiety.model;

import java.math.BigDecimal;

public record CallAnxietyCalculation(
        BigDecimal performanceScore,
        BigDecimal performanceRiskScore,
        BigDecimal subjectiveAnxietyScore,
        BigDecimal trainingStateIndex,
        BigDecimal newCurrentCallAnxietyIndex
) {
}
