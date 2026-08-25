package ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request;

import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.AnalysisQualityStatus;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TrainingAnalysisRequest(
        BigDecimal stabilityScore,
        BigDecimal conversationScore,
        BigDecimal fluencyScore,
        Long userSpeechDurationMs,
        Long aiSpeechDurationMs,
        Long serverWaitDurationMs,
        Integer validUserTurnCount,
        Long userTremorDurationMs,
        Long userSustainedSpeechDurationMs,
        Integer completedScriptSteps,
        Integer scriptStepCount,
        AnalysisQualityStatus analysisQualityStatus,
        String analysisExclusionReason,
        String analyzerVersion,
        String analysisPolicyVersion
) {
}
