package ChickenMayoDeopbab.bada.domain.attendance.service;

import ChickenMayoDeopbab.bada.domain.attendance.dto.response.GetAttendanceResponse;
import ChickenMayoDeopbab.bada.domain.attendance.entity.Attendance;
import ChickenMayoDeopbab.bada.domain.attendance.exception.AttendanceStatusCode;
import ChickenMayoDeopbab.bada.domain.attendance.repository.AttendanceQueryRepository;
import ChickenMayoDeopbab.bada.domain.attendance.repository.AttendanceRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final UsersRepository usersRepository;
    private final AttendanceQueryRepository attendanceQueryRepository;

    public ApiResponse<Boolean> attend() {
        Users userInfo = getUserInfo();
        LocalDate now = LocalDate.now();
        if (attendanceRepository.existsByAttendedDateAndUser(now, userInfo)) {
            throw new ApplicationException(AttendanceStatusCode.ALREADY_ATTENDED);
        }
        attendanceRepository.save(Attendance.builder()
                .user(userInfo)
                .build());

        return ApiResponse.ok(Boolean.TRUE, "출석이 되었습니다.");
    }

    public ApiResponse<List<GetAttendanceResponse>> getMonthlyAttendance(int year,int month) {
        Users userInfo = getUserInfo();
        List<Attendance> attendances = attendanceQueryRepository.getAttendanceByYearAndMonth(year, month, userInfo);
        return ApiResponse.ok(GetAttendanceResponse.fromList(attendances));
    }

    private Users getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return usersRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));
    }
}
