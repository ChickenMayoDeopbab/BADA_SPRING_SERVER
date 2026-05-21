package ChickenMayoDeopbab.bada.domain.attendance.repository;

import ChickenMayoDeopbab.bada.domain.attendance.entity.Attendance;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;

import java.util.List;

public interface AttendanceQueryRepository {
    List<Attendance> getAttendanceByYearAndMonth(int year, int month, Users user);
}
