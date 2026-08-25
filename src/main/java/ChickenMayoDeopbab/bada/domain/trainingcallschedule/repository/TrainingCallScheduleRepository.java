package ChickenMayoDeopbab.bada.domain.trainingcallschedule.repository;

import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallSchedule;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallScheduleStatus;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TrainingCallScheduleRepository extends JpaRepository<TrainingCallSchedule, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TrainingCallSchedule> findByScheduleIdAndUser(Long scheduleId, Users user);

    Optional<TrainingCallSchedule> findFirstByUserAndStatusOrderByTriggeredAtDesc(
            Users user,
            TrainingCallScheduleStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<TrainingCallSchedule> findTop100ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            TrainingCallScheduleStatus status,
            LocalDateTime now
    );

    @Modifying(flushAutomatically = true)
    @Query("delete from TrainingCallSchedule s where s.user = :user")
    void deleteAllByUser(@Param("user") Users user);
}
