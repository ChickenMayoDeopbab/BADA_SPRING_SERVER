package ChickenMayoDeopbab.bada.domain.adminstatistics.entity;

import ChickenMayoDeopbab.bada.domain.adminstatistics.model.EvaluationPolicy;
import ChickenMayoDeopbab.bada.domain.adminstatistics.model.EvaluationResult;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "evaluation_runs")
@Getter
@NoArgsConstructor
public class EvaluationRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime asOf;
    private String policyVersion;
    private boolean invalidated;
    @Version private Long version;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb", nullable = false)
    private EvaluationPolicy policy;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb", nullable = false)
    private List<EvaluationResult> results;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb", nullable = false)
    private Map<Long, String> exclusions;

    public EvaluationRun(String name, Long createdBy, LocalDateTime asOf, String policyVersion,
            EvaluationPolicy policy, List<EvaluationResult> results, Map<Long, String> exclusions) {
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.asOf = asOf;
        this.policyVersion = policyVersion;
        this.policy = policy;
        this.results = results;
        this.exclusions = exclusions;
    }

    public void eraseParticipant(Long userId) {
        if (results.stream().noneMatch(result -> result.userId().equals(userId)) && !exclusions.containsKey(userId)) { return; }
        results = results.stream().filter(result -> !result.userId().equals(userId)).toList();
        var retainedExclusions = new java.util.HashMap<>(exclusions);
        retainedExclusions.remove(userId);
        exclusions = Map.copyOf(retainedExclusions);
        invalidated = true;
    }
}
