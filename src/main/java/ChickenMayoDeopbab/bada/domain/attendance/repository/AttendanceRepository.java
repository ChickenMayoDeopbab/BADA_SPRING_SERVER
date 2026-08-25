package ChickenMayoDeopbab.bada.domain.attendance.repository;

import ChickenMayoDeopbab.bada.domain.attendance.entity.Attendance;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    boolean existsByAttendedDateAndUser(LocalDate attendedDate, Users user);

    @Modifying(flushAutomatically = true)
    @Query("delete from Attendance a where a.user = :user")
    void deleteAllByUser(@Param("user") Users user);
}
