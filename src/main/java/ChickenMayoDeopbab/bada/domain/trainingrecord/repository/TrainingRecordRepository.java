package ChickenMayoDeopbab.bada.domain.trainingrecord.repository;

import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, Long> {

    boolean existsBySessionId(String sessionId);

    Optional<TrainingRecord> findByRecordIdAndUser(Long recordId, Users user);

    Page<TrainingRecord> findByUserOrderByStartedAtDesc(Users user, Pageable pageable);

    Optional<TrainingRecord> findFirstByScenarioIdAndUserOrderByEndedAtDesc(Long scenarioId, Users user);

    long countByUserAndScenarioIdAndEndReasonNotIn(Users user, Long scenarioId, Collection<EndReason> endReasons);
}
