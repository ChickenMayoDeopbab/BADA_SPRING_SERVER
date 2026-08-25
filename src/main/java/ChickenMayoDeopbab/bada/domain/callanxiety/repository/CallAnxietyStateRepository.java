package ChickenMayoDeopbab.bada.domain.callanxiety.repository;

import ChickenMayoDeopbab.bada.domain.adminstatistics.model.CallAnxietyStateStatisticsRow;
import ChickenMayoDeopbab.bada.domain.callanxiety.entity.CallAnxietyState;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CallAnxietyStateRepository extends JpaRepository<CallAnxietyState, Long> {
    Optional<CallAnxietyState> findByUser(Users user);

    boolean existsByUser(Users user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select state
            from CallAnxietyState state
            where state.user = :user
            """)
    Optional<CallAnxietyState> findByUserForUpdate(
            @Param("user") Users user
    );

    @Query("""
        select new ChickenMayoDeopbab.bada.domain.adminstatistics.model.CallAnxietyStateStatisticsRow(
            state.user.userId,
            state.initialSelfReportScore,
            state.currentCallAnxietyIndex,
            state.initialLevel,
            state.currentLevel,
            state.validTrainingCount,
            state.scoringVersion
        )
        from CallAnxietyState state
        order by state.user.userId asc
        """)
    List<CallAnxietyStateStatisticsRow> findAllForAdminStatistics();

    @Modifying(flushAutomatically = true)
    @Query("delete from CallAnxietyState state where state.user = :user")
    void deleteByUser(@Param("user") Users user);
}
