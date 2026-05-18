package ChickenMayoDeopbab.bada.domain.attendance.repository;

import ChickenMayoDeopbab.bada.domain.attendance.entity.Attendance;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    boolean existsByAttendedDateAndUser(LocalDate attendedDate, Users user);
}
