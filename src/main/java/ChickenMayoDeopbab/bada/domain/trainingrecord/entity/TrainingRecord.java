package ChickenMayoDeopbab.bada.domain.trainingrecord.entity;

import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "training_records")
public class TrainingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false, unique = true)
    private String sessionId;

    private Long scenarioId;

    @Column(nullable = false)
    private String scenarioName;

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

    @Builder
    private TrainingRecord(
            Users user,
            String sessionId,
            Long scenarioId,
            String scenarioName,
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
    }
}
