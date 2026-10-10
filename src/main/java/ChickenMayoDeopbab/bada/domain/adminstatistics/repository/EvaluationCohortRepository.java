package ChickenMayoDeopbab.bada.domain.adminstatistics.repository;

import ChickenMayoDeopbab.bada.domain.adminstatistics.entity.EvaluationCohort;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import java.util.Optional;

public interface EvaluationCohortRepository extends JpaRepository<EvaluationCohort, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cohort from EvaluationCohort cohort where cohort.id = :id")
    Optional<EvaluationCohort> findForUpdate(Long id);

    @Modifying
    @Query("delete from EvaluationParticipant participant where participant.userId = :userId")
    void removeParticipantFromAllCohorts(Long userId);
}
