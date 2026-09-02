package ChickenMayoDeopbab.bada.domain.trainingrecord.repository;

import ChickenMayoDeopbab.bada.domain.adminstatistics.model.AppliedTrainingStatisticsRow;
import ChickenMayoDeopbab.bada.domain.adminstatistics.model.AppliedTrainingTimelineRow;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.projection.ScenarioCategoryProjection;
import ChickenMayoDeopbab.bada.domain.user.entity.Role;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, Long> {

    boolean existsBySessionId(String sessionId);

    Optional<TrainingRecord> findByRecordIdAndUser(Long recordId, Users user);

    Page<TrainingRecord> findByUserOrderByStartedAtDesc(Users user, Pageable pageable);

    @Query(value = """
        select scenario_id as scenarioId,
               category as category
        from scenario
        where scenario_id in (:scenarioIds)
        """, nativeQuery = true)
    List<ScenarioCategoryProjection> findScenarioCategoriesByIds(
            @Param("scenarioIds") Collection<Long> scenarioIds
    );

    Optional<TrainingRecord> findFirstByScenarioIdAndUserOrderByEndedAtDesc(Long scenarioId, Users user);

    long countByUserAndScenarioIdAndEndReasonNotIn(Users user, Long scenarioId, Collection<EndReason> endReasons);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TrainingRecord> findBySessionIdAndUser(String sessionId, Users user);

    int countByUser(Users user);

    List<TrainingRecord> findAllByUser(Users user);

    @Query("""
        select new ChickenMayoDeopbab.bada.domain.adminstatistics.model.AppliedTrainingStatisticsRow(
            training.user.userId,
            training.difficulty,
            training.aiPersonality,
            training.analysis.analyzerVersion,
            training.scoringVersion,
            training.trainingStateIndex,
            training.scoreSequence
        )
        from TrainingRecord training
        where training.scoreApplied = true
        order by training.user.userId asc,
                 training.scoreSequence desc
        """)
    List<AppliedTrainingStatisticsRow> findAllAppliedForAdminStatistics();

    @Query("""
        select new ChickenMayoDeopbab.bada.domain.adminstatistics.model.AppliedTrainingTimelineRow(
            training.user.userId,
            training.scoreAppliedAt
        )
        from TrainingRecord training
        where training.scoreApplied = true
          and training.scoreAppliedAt is not null
          and training.scoringVersion = :scoringVersion
        order by training.user.userId asc,
                 training.scoreAppliedAt asc
        """)
    List<AppliedTrainingTimelineRow> findAllAppliedTimelinesForAdminStatistics(
            @Param("scoringVersion") String scoringVersion
    );

    @Query("""
        select count(distinct training.user.userId)
        from TrainingRecord training
        where training.user.role = :role
          and training.endReason not in :excludedEndReasons
        """)
    long countDistinctTrainedUsers(
            @Param("role") Role role,
            @Param("excludedEndReasons")
            Collection<EndReason> excludedEndReasons
    );

    @Query("""
        select count(distinct training.user.userId)
        from TrainingRecord training
        where training.user.role = :role
          and training.user.paymentIntended = true
          and training.endReason not in :excludedEndReasons
        """)
    long countDistinctPaymentIntendedUsers(
            @Param("role") Role role,
            @Param("excludedEndReasons")
            Collection<EndReason> excludedEndReasons
    );
    List<TrainingRecord>
    findAllByUserAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtAsc(
            Users user,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );
}
