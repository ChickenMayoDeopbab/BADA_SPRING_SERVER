package ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity;

import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
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
@Table(name = "training_call_schedules")
public class TrainingCallSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false)
    private Long scenarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiPersonality aiPersonality;

    @Column(nullable = false)
    private Integer minDelayMinutes;

    @Column(nullable = false)
    private Integer maxDelayMinutes;

    private Integer maxDurationSeconds;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainingCallScheduleStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime triggeredAt;

    private LocalDateTime acceptedAt;

    private String sessionId;

    @Builder
    private TrainingCallSchedule(
            Users user,
            Long scenarioId,
            SessionType type,
            AiPersonality aiPersonality,
            Integer minDelayMinutes,
            Integer maxDelayMinutes,
            Integer maxDurationSeconds,
            LocalDateTime scheduledAt
    ) {
        this.user = user;
        this.scenarioId = scenarioId;
        this.type = type;
        this.aiPersonality = aiPersonality;
        this.minDelayMinutes = minDelayMinutes;
        this.maxDelayMinutes = maxDelayMinutes;
        this.maxDurationSeconds = maxDurationSeconds;
        this.scheduledAt = scheduledAt;
        this.status = TrainingCallScheduleStatus.SCHEDULED;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(Users user) {
        return this.user != null && user != null && this.user.getUserId().equals(user.getUserId());
    }

    public void markRinging(LocalDateTime triggeredAt) {
        this.status = TrainingCallScheduleStatus.RINGING;
        this.triggeredAt = triggeredAt;
    }

    public void accept(String sessionId, LocalDateTime acceptedAt) {
        this.status = TrainingCallScheduleStatus.ACCEPTED;
        this.sessionId = sessionId;
        this.acceptedAt = acceptedAt;
    }

    public void cancel() {
        this.status = TrainingCallScheduleStatus.CANCELED;
    }
}
