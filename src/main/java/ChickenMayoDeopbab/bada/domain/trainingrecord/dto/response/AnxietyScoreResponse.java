package ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response;

import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;

import java.math.BigDecimal;

public record AnxietyScoreResponse(
        Long recordId,
        String sessionId,
        Short anxietyScore,
        Boolean scoreApplied,
        String scoreExclusionReason,
        BigDecimal performanceScore,
        BigDecimal performanceRiskScore,
        BigDecimal subjectiveAnxietyScore,
        BigDecimal trainingStateIndex,
        BigDecimal scoreBefore,
        BigDecimal scoreAfter,

        Long scoreSequence,
        String scoringVersion
) {
    public static AnxietyScoreResponse from(
            TrainingRecord record
    ) {
        return new AnxietyScoreResponse(
                record.getRecordId(),
                record.getSessionId(),
                record.getAnxietyScore(),
                record.getScoreApplied(),
                record.getScoreExclusionReason(),
                record.getPerformanceScore(),
                record.getPerformanceRiskScore(),
                record.getSubjectiveAnxietyScore(),
                record.getTrainingStateIndex(),
                record.getScoreBefore(),
                record.getScoreAfter(),
                record.getScoreSequence(),
                record.getScoringVersion()
        );
    }
}
