package ChickenMayoDeopbab.bada.domain.trainingrecord.entity;

import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request.TrainingAnalysisRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Embeddable
@NoArgsConstructor
public class TrainingAnalysisMetrics {
    @Column(name = "stability_score", precision = 5, scale = 2)
    private BigDecimal stabilityScore;

    @Column(name = "conversation_score", precision = 5, scale = 2)
    private BigDecimal conversationScore;

    @Column(name = "fluency_score", precision = 5, scale = 2)
    private BigDecimal fluencyScore;

    @Column(name = "user_speech_duration_ms")
    private Long userSpeechDurationMs;

    @Column(name = "ai_speech_duration_ms")
    private Long aiSpeechDurationMs;

    @Column(name = "server_wait_duration_ms")
    private Long serverWaitDurationMs;

    @Column(name = "valid_user_turn_count")
    private Integer validUserTurnCount;

    @Column(name = "user_tremor_duration_ms")
    private Long userTremorDurationMs;

    @Column(name = "user_sustained_speech_duration_ms")
    private Long userSustainedSpeechDurationMs;

    @Column(name = "completed_script_steps")
    private Integer completedScriptSteps;

    @Column(name = "script_step_count")
    private Integer scriptStepCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_quality_status")
    private AnalysisQualityStatus analysisQualityStatus;

    @Column(name = "analysis_exclusion_reason", length = 100)
    private String analysisExclusionReason;

    @Column(name = "analyzer_version", length = 50)
    private String analyzerVersion;

    @Column(name = "analysis_policy_version", length = 50)
    private String analysisPolicyVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, AnalysisQualityStatus> metricQuality;
    private Integer unansweredUserTurnCount;
    private Integer unrecognizedUserTurnCount;
    private Long leadingSilenceDurationMs;
    private Long trailingSilenceDurationMs;

    private TrainingAnalysisMetrics(TrainingAnalysisRequest request) {
        this.stabilityScore = request.stabilityScore();
        this.conversationScore = request.conversationScore();
        this.fluencyScore = request.fluencyScore();
        this.userSpeechDurationMs = request.userSpeechDurationMs();
        this.aiSpeechDurationMs = request.aiSpeechDurationMs();
        this.serverWaitDurationMs = request.serverWaitDurationMs();
        this.validUserTurnCount = request.validUserTurnCount();
        this.userTremorDurationMs = request.userTremorDurationMs();
        this.userSustainedSpeechDurationMs =
                request.userSustainedSpeechDurationMs();
        this.completedScriptSteps = request.completedScriptSteps();
        this.scriptStepCount = request.scriptStepCount();
        this.analysisQualityStatus = request.analysisQualityStatus();
        this.analysisExclusionReason = request.analysisExclusionReason();
        this.analyzerVersion = request.analyzerVersion();
        this.analysisPolicyVersion = request.analysisPolicyVersion();
        this.metricQuality = request.metricQuality();
        this.unansweredUserTurnCount = request.unansweredUserTurnCount();
        this.unrecognizedUserTurnCount = request.unrecognizedUserTurnCount();
        this.leadingSilenceDurationMs = request.leadingSilenceDurationMs();
        this.trailingSilenceDurationMs = request.trailingSilenceDurationMs();
    }

    public static TrainingAnalysisMetrics from(
            TrainingAnalysisRequest request
    ) {
        if (request == null) {
            return null;
        }
        return new TrainingAnalysisMetrics(request);
    }

    public boolean isPassed() {
        return analysisQualityStatus == AnalysisQualityStatus.PASS;
    }

    public boolean isMetricPassed(String metric) {
        return metricQuality == null || metricQuality.isEmpty() ? isPassed()
                : metricQuality.get(metric) == AnalysisQualityStatus.PASS;
    }

    public boolean hasObjectiveScores() {
        return stabilityScore != null
                && conversationScore != null
                && fluencyScore != null;
    }

    public boolean hasValidObjectiveScores() {
        return isScoreInRange(stabilityScore)
                && isScoreInRange(conversationScore)
                && isScoreInRange(fluencyScore);
    }

    public boolean hasVersions() {
        return analyzerVersion != null
                && !analyzerVersion.isBlank()
                && analysisPolicyVersion != null
                && !analysisPolicyVersion.isBlank();
    }

    private boolean isScoreInRange(BigDecimal score) {
        if (score == null) {
            return false;
        }

        return score.compareTo(BigDecimal.ZERO) >= 0
                && score.compareTo(new BigDecimal("100")) <= 0;
    }
}
