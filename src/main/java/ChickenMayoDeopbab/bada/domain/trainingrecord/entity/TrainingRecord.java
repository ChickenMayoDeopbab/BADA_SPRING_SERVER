package ChickenMayoDeopbab.bada.domain.trainingrecord.entity;

import ChickenMayoDeopbab.bada.domain.callanxiety.model.CallAnxietyCalculation;
import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "training_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_training_record_session",
                        columnNames = "session_id"
                ),
                @UniqueConstraint(
                        name = "uk_training_record_user_score_sequence",
                        columnNames = {
                                "user_id",
                                "score_sequence"
                        }
                )
        }
)
public class TrainingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false)
    private String sessionId;

    private Long scenarioId;

    @Column(nullable = false)
    private String scenarioName;

    private String scenarioVersion;

    private String difficulty;
    private Integer scriptLevel;

    @Embedded
    private TrainingAnalysisMetrics analysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "speech_metrics", columnDefinition = "jsonb")
    private TrainingSpeechMetrics speechMetrics;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionType sessionType;

    @Enumerated(EnumType.STRING)
    private AiPersonality aiPersonality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EndReason endReason;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime endedAt;

    @Column(nullable = false)
    private Long durationSeconds;

    @Lob
    private String transcript;

    @Lob
    private String goodSegments;

    private String recordingKey;

    private Short anxietyScore;
    private LocalDateTime anxietyRecordedAt;

    @Column(precision = 7, scale = 4)
    private BigDecimal performanceScore;

    @Column(precision = 6, scale = 4)
    private BigDecimal performanceRiskScore;

    @Column(precision = 6, scale = 4)
    private BigDecimal subjectiveAnxietyScore;

    @Column(precision = 6, scale = 4)
    private BigDecimal trainingStateIndex;

    @Column(precision = 6, scale = 4)
    private BigDecimal scoreBefore;

    @Column(precision = 6, scale = 4)
    private BigDecimal scoreAfter;

    private Boolean scoreApplied;

    private LocalDateTime scoreAppliedAt;

    private String scoreExclusionReason;

    private Long scoreSequence;

    private String scoringVersion;

    @Builder
    private TrainingRecord(
            Users user,
            String sessionId,
            Long scenarioId,
            String scenarioName,
            String scenarioVersion,
            String difficulty,
            Integer scriptLevel,
            TrainingAnalysisMetrics analysis,
            TrainingSpeechMetrics speechMetrics,
            SessionType sessionType,
            AiPersonality aiPersonality,
            EndReason endReason,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            Long durationSeconds,
            String transcript,
            String goodSegments,
            String recordingKey
    ) {
        this.user = user;
        this.sessionId = sessionId;
        this.scenarioId = scenarioId;
        this.scenarioName = scenarioName;
        this.scenarioVersion = scenarioVersion;
        this.difficulty = difficulty;
        this.scriptLevel = scriptLevel;
        this.analysis = analysis;
        this.speechMetrics = speechMetrics;
        this.scoreApplied = false;
        this.sessionType = sessionType;
        this.aiPersonality = aiPersonality;
        this.endReason = endReason;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationSeconds = durationSeconds;
        this.transcript = transcript;
        this.goodSegments = goodSegments;
        this.recordingKey = recordingKey;
    }

    public void recordAnxietyScore(Short score) {
        this.anxietyScore = score;
        this.anxietyRecordedAt = LocalDateTime.now();
    }


    public boolean hasSameAnxietyScore(Short anxietyScore) {
        return this.anxietyScore != null
                && this.anxietyScore.equals(anxietyScore);
    }

    public boolean isScoreProcessed() {
        return Boolean.TRUE.equals(scoreApplied)
                || (
                scoreExclusionReason != null
                        && !scoreExclusionReason.isBlank()
        );
    }

    public boolean isScoreApplied() {
        return Boolean.TRUE.equals(scoreApplied);
    }

    public void applyScore(
            Short anxietyScore,
            CallAnxietyCalculation calculation,
            BigDecimal scoreBefore,
            long scoreSequence,
            String scoringVersion,
            LocalDateTime appliedAt
    ) {
        this.anxietyScore = anxietyScore;
        if (this.anxietyRecordedAt == null) { this.anxietyRecordedAt = appliedAt; }

        this.performanceScore =
                calculation.performanceScore();

        this.performanceRiskScore =
                calculation.performanceRiskScore();

        this.subjectiveAnxietyScore =
                calculation.subjectiveAnxietyScore();

        this.trainingStateIndex =
                calculation.trainingStateIndex();

        this.scoreBefore = scoreBefore;

        this.scoreAfter =
                calculation.newCurrentCallAnxietyIndex();

        this.scoreApplied = true;
        this.scoreAppliedAt = appliedAt;
        this.scoreExclusionReason = null;
        this.scoreSequence = scoreSequence;
        this.scoringVersion = scoringVersion;
    }

    public void excludeScore(
            Short anxietyScore,
            String exclusionReason,
            String scoringVersion
    ) {
        this.anxietyScore = anxietyScore;
        if (this.anxietyRecordedAt == null) { this.anxietyRecordedAt = LocalDateTime.now(); }
        this.scoreApplied = false;
        this.scoreAppliedAt = null;
        this.scoreExclusionReason = exclusionReason;
        this.scoreSequence = null;
        this.scoringVersion = scoringVersion;
    }
}
