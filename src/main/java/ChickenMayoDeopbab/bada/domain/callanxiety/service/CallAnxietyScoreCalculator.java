package ChickenMayoDeopbab.bada.domain.callanxiety.service;

import ChickenMayoDeopbab.bada.domain.callanxiety.model.CallAnxietyCalculation;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Component
public class CallAnxietyScoreCalculator {
    private static final int INTERNAL_SCALE = 10;
    private static final int STORAGE_SCALE = 4;

    private static final BigDecimal MIN_INDEX = new BigDecimal("1.0");
    private static final BigDecimal MAX_INDEX = new BigDecimal("5.0");

    private static final BigDecimal OBJECTIVE_CONVERSION =
            new BigDecimal("0.04");

    private static final BigDecimal SUBJECTIVE_BASE =
            new BigDecimal("1.0");

    private static final BigDecimal SUBJECTIVE_CONVERSION =
            new BigDecimal("0.4");

    private static final BigDecimal PERFORMANCE_WEIGHT =
            new BigDecimal("0.4");

    private static final BigDecimal SUBJECTIVE_WEIGHT =
            new BigDecimal("0.6");

    private static final BigDecimal PREVIOUS_INDEX_WEIGHT =
            new BigDecimal("0.9");

    private static final BigDecimal TRAINING_INDEX_WEIGHT =
            new BigDecimal("0.1");

    public CallAnxietyCalculation calculate(
            BigDecimal currentCallAnxietyIndex,
            BigDecimal stabilityScore,
            BigDecimal conversationScore,
            BigDecimal fluencyScore,
            int subjectiveAnxietyInput
    ) {
        validateRange(
                "currentCallAnxietyIndex",
                currentCallAnxietyIndex,
                MIN_INDEX,
                MAX_INDEX
        );

        validateRange(
                "stabilityScore",
                stabilityScore,
                BigDecimal.ZERO,
                new BigDecimal("100")
        );

        validateRange(
                "conversationScore",
                conversationScore,
                BigDecimal.ZERO,
                new BigDecimal("100")
        );

        validateRange(
                "fluencyScore",
                fluencyScore,
                BigDecimal.ZERO,
                new BigDecimal("100")
        );

        if (subjectiveAnxietyInput < 0 || subjectiveAnxietyInput > 10) {
            throw new IllegalArgumentException(
                    "subjectiveAnxietyInput은 0~10 범위여야 합니다."
            );
        }

        BigDecimal performanceScore = stabilityScore
                .add(conversationScore)
                .add(fluencyScore)
                .divide(
                        new BigDecimal("3"),
                        INTERNAL_SCALE,
                        RoundingMode.HALF_UP
                );

        BigDecimal performanceRiskScore = clamp(
                MAX_INDEX.subtract(
                        performanceScore.multiply(OBJECTIVE_CONVERSION)
                ),
                MIN_INDEX,
                MAX_INDEX
        );

        BigDecimal subjectiveAnxietyScore = clamp(
                SUBJECTIVE_BASE.add(
                        BigDecimal.valueOf(subjectiveAnxietyInput)
                                .multiply(SUBJECTIVE_CONVERSION)
                ),
                MIN_INDEX,
                MAX_INDEX
        );

        BigDecimal trainingStateIndex = performanceRiskScore
                .multiply(PERFORMANCE_WEIGHT)
                .add(
                        subjectiveAnxietyScore.multiply(SUBJECTIVE_WEIGHT)
                );

        BigDecimal newCurrentCallAnxietyIndex =
                currentCallAnxietyIndex
                        .multiply(PREVIOUS_INDEX_WEIGHT)
                        .add(
                                trainingStateIndex.multiply(
                                        TRAINING_INDEX_WEIGHT
                                )
                        );

        return new CallAnxietyCalculation(
                normalize(performanceScore),
                normalize(performanceRiskScore),
                normalize(subjectiveAnxietyScore),
                normalize(trainingStateIndex),
                normalize(
                        clamp(
                                newCurrentCallAnxietyIndex,
                                MIN_INDEX,
                                MAX_INDEX
                        )
                )
        );
    }

    public CallPhobiaLevel calculateLevel(BigDecimal score) {
        validateRange("score", score, MIN_INDEX, MAX_INDEX);

        if (score.compareTo(new BigDecimal("1.5")) < 0) {
            return CallPhobiaLevel.LEVEL_1;
        }
        if (score.compareTo(new BigDecimal("2.5")) < 0) {
            return CallPhobiaLevel.LEVEL_2;
        }
        if (score.compareTo(new BigDecimal("3.5")) < 0) {
            return CallPhobiaLevel.LEVEL_3;
        }
        if (score.compareTo(new BigDecimal("4.5")) < 0) {
            return CallPhobiaLevel.LEVEL_4;
        }
        return CallPhobiaLevel.LEVEL_5;
    }

    private BigDecimal clamp(
            BigDecimal value,
            BigDecimal minimum,
            BigDecimal maximum
    ) {
        if (value.compareTo(minimum) < 0) {
            return minimum;
        }
        if (value.compareTo(maximum) > 0) {
            return maximum;
        }
        return value;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.setScale(STORAGE_SCALE, RoundingMode.HALF_UP);
    }

    private void validateRange(
            String field,
            BigDecimal value,
            BigDecimal minimum,
            BigDecimal maximum
    ) {
        Objects.requireNonNull(value, field + "는 null일 수 없습니다.");

        if (
                value.compareTo(minimum) < 0
                        || value.compareTo(maximum) > 0
        ) {
            throw new IllegalArgumentException(
                    field + "는 " + minimum + "~" + maximum
                            + " 범위여야 합니다."
            );
        }
    }
}
