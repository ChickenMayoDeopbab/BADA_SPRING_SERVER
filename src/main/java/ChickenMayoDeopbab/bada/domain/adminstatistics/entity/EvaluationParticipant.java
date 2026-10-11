package ChickenMayoDeopbab.bada.domain.adminstatistics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_participants", uniqueConstraints =
        @UniqueConstraint(name = "uk_evaluation_cohort_user", columnNames = {"cohort_id", "user_id"}))
@Getter
@NoArgsConstructor
public class EvaluationParticipant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cohort_id")
    private EvaluationCohort cohort;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false)
    private String source;
    private LocalDateTime startedAt;
    private LocalDateTime endOverride;

    public EvaluationParticipant(EvaluationCohort cohort, Long userId, String source,
            LocalDateTime startedAt, LocalDateTime endOverride) {
        this.cohort = cohort;
        this.userId = userId;
        this.source = source;
        this.startedAt = startedAt;
        this.endOverride = endOverride;
    }
}
