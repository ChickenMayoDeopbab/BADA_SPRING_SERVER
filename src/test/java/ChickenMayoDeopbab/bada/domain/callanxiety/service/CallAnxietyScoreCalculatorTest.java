package ChickenMayoDeopbab.bada.domain.callanxiety.service;

import ChickenMayoDeopbab.bada.domain.callanxiety.model.CallAnxietyCalculation;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CallAnxietyScoreCalculatorTest {

    private final CallAnxietyScoreCalculator calculator =
            new CallAnxietyScoreCalculator();

    @Test
    void calculatesTrainingAndCurrentIndex() {
        CallAnxietyCalculation result = calculator.calculate(
                new BigDecimal("3.0"),
                new BigDecimal("75"),
                new BigDecimal("75"),
                new BigDecimal("75"),
                5
        );

        assertThat(result.performanceScore())
                .isEqualByComparingTo("75.0000");

        assertThat(result.performanceRiskScore())
                .isEqualByComparingTo("2.0000");

        assertThat(result.subjectiveAnxietyScore())
                .isEqualByComparingTo("3.0000");

        assertThat(result.trainingStateIndex())
                .isEqualByComparingTo("2.6000");

        assertThat(result.newCurrentCallAnxietyIndex())
                .isEqualByComparingTo("2.9600");
    }

    @Test
    void limitsMaximumChangeToPointFour() {
        CallAnxietyCalculation result = calculator.calculate(
                new BigDecimal("5.0"),
                new BigDecimal("100"),
                new BigDecimal("100"),
                new BigDecimal("100"),
                0
        );

        assertThat(result.trainingStateIndex())
                .isEqualByComparingTo("1.0000");

        assertThat(result.newCurrentCallAnxietyIndex())
                .isEqualByComparingTo("4.6000");
    }

    @Test
    void calculatesLevelUsingExplicitBoundaries() {
        assertThat(calculator.calculateLevel(new BigDecimal("1.4999")))
                .isEqualTo(CallPhobiaLevel.LEVEL_1);

        assertThat(calculator.calculateLevel(new BigDecimal("1.5000")))
                .isEqualTo(CallPhobiaLevel.LEVEL_2);

        assertThat(calculator.calculateLevel(new BigDecimal("2.5000")))
                .isEqualTo(CallPhobiaLevel.LEVEL_3);

        assertThat(calculator.calculateLevel(new BigDecimal("3.5000")))
                .isEqualTo(CallPhobiaLevel.LEVEL_4);

        assertThat(calculator.calculateLevel(new BigDecimal("4.5000")))
                .isEqualTo(CallPhobiaLevel.LEVEL_5);
    }

    @Test
    void rejectsOutOfRangeScore() {
        assertThatThrownBy(() -> calculator.calculate(
                new BigDecimal("3.0"),
                new BigDecimal("101"),
                new BigDecimal("75"),
                new BigDecimal("75"),
                5
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
