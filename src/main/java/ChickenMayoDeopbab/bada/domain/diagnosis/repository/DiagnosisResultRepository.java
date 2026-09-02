package ChickenMayoDeopbab.bada.domain.diagnosis.repository;

import ChickenMayoDeopbab.bada.domain.adminstatistics.model.SelfAssessmentStatisticsRow;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisResult;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisType;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiagnosisResultRepository extends JpaRepository<DiagnosisResult, Integer> {
    Optional<DiagnosisResult> findFirstByUserOrderByCreatedAtDescQuestionIdDesc(
            Users user
    );

    @Query("""
        select new ChickenMayoDeopbab.bada.domain.adminstatistics.model.SelfAssessmentStatisticsRow(
            diagnosis.questionId,
            diagnosis.user.userId,
            diagnosis.score,
            diagnosis.createdAt
        )
        from DiagnosisResult diagnosis
        where diagnosis.user is not null
          and diagnosis.type = :type
        order by diagnosis.user.userId asc,
                 diagnosis.createdAt asc,
                 diagnosis.questionId asc
        """)
    List<SelfAssessmentStatisticsRow> findAllForAdminStatistics(
            @Param("type") DiagnosisType type
    );

    @Modifying(flushAutomatically = true)
    @Query("delete from DiagnosisResult d where d.user = :user")
    void deleteAllByUser(@Param("user") Users user);
}
