package ChickenMayoDeopbab.bada.domain.adminstatistics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluation_cohorts")
@Getter
@NoArgsConstructor
public class EvaluationCohort {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    private LocalDateTime createdAt;
    @Version private Long version;
    @OneToMany(mappedBy = "cohort", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluationParticipant> participants = new ArrayList<>();

    public EvaluationCohort(String name) {
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }
    public void replaceParticipants(List<EvaluationParticipant> members) {
        participants.clear();
        participants.addAll(members);
    }
}
