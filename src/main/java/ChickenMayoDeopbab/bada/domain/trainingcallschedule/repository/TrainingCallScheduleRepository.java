package ChickenMayoDeopbab.bada.domain.trainingcallschedule.repository;

import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallSchedule;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrainingCallScheduleRepository extends JpaRepository<TrainingCallSchedule, Long> {

    Optional<TrainingCallSchedule> findByScheduleIdAndUser(Long scheduleId, Users user);
}
