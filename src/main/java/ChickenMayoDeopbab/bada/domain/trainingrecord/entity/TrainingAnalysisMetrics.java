package ChickenMayoDeopbab.bada.domain.trainingrecord.entity;

import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request.TrainingAnalysisRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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

}
