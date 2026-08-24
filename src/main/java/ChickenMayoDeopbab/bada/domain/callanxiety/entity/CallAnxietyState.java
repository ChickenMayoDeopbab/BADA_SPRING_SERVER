package ChickenMayoDeopbab.bada.domain.callanxiety.entity;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "call_anxiety_states",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_call_anxiety_state_user",
                        columnNames = "user_id"
                )
        }
)
@NoArgsConstructor
public class CallAnxietyState {
    public static final String SELF_ASSESSMENT_VERSION =
            "SELF_ASSESSMENT_V1";

    public static final String SCORING_VERSION =
            "CALL_ANXIETY_V1";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stateId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Users user;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal initialSelfReportScore;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal currentCallAnxietyIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallPhobiaLevel initialLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallPhobiaLevel currentLevel;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CallPhobiaLevel candidateLevel;

    @Column(nullable = false)
    private int candidateLevelCount;

    @Column(nullable = false)
    private int validTrainingCount;

    @Column(nullable = false, length = 50)
    private String selfAssessmentVersion;

    @Column(nullable = false, length = 50)
    private String scoringVersion;

    @Column(nullable = false)
    private LocalDateTime diagnosedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    private CallAnxietyState(
            Users user,
            BigDecimal initialSelfReportScore,
            CallPhobiaLevel initialLevel,
            LocalDateTime diagnosedAt
    ) {
        BigDecimal normalizedScore = initialSelfReportScore.setScale(
                4,
                RoundingMode.HALF_UP
        );

        this.user = user;
        this.initialSelfReportScore = normalizedScore;
        this.currentCallAnxietyIndex = normalizedScore;
        this.initialLevel = initialLevel;
        this.currentLevel = initialLevel;
        this.candidateLevel = null;
        this.candidateLevelCount = 0;
        this.validTrainingCount = 0;
        this.selfAssessmentVersion = SELF_ASSESSMENT_VERSION;
        this.scoringVersion = SCORING_VERSION;
        this.diagnosedAt = diagnosedAt;
        this.updatedAt = LocalDateTime.now();
    }

    public static CallAnxietyState create(
            Users user,
            BigDecimal initialSelfReportScore,
            CallPhobiaLevel initialLevel,
            LocalDateTime diagnosedAt
    ) {
        if (user == null) {
            throw new IllegalArgumentException("user는 null일 수 없습니다.");
        }
        if (initialSelfReportScore == null) {
            throw new IllegalArgumentException(
                    "initialSelfReportScore는 null일 수 없습니다."
            );
        }
        if (initialLevel == null) {
            throw new IllegalArgumentException(
                    "initialLevel은 null일 수 없습니다."
            );
        }

        return new CallAnxietyState(
                user,
                initialSelfReportScore,
                initialLevel,
                diagnosedAt != null ? diagnosedAt : LocalDateTime.now()
        );
    }

    // 실제 점수 반영 단계에서 사용
    public void applyValidTraining(
            BigDecimal newCurrentIndex,
            CallPhobiaLevel calculatedLevel
    ) {
        this.currentCallAnxietyIndex = newCurrentIndex.setScale(
                4,
                RoundingMode.HALF_UP
        );
        this.validTrainingCount += 1;

        updateLevelCandidate(calculatedLevel);
        this.updatedAt = LocalDateTime.now();
    }

    private void updateLevelCandidate(CallPhobiaLevel calculatedLevel) {
        if (calculatedLevel == currentLevel) {
            clearCandidate();
            return;
        }

        if (calculatedLevel == candidateLevel) {
            candidateLevelCount += 1;
        } else {
            candidateLevel = calculatedLevel;
            candidateLevelCount = 1;
        }

        if (
                validTrainingCount >= 3
                        && candidateLevelCount >= 2
        ) {
            currentLevel = candidateLevel;
            clearCandidate();
        }
    }

    private void clearCandidate() {
        candidateLevel = null;
        candidateLevelCount = 0;
    }
}
