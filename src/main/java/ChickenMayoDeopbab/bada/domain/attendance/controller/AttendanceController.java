package ChickenMayoDeopbab.bada.domain.attendance.controller;

import ChickenMayoDeopbab.bada.domain.attendance.dto.response.GetAttendanceResponse;
import ChickenMayoDeopbab.bada.domain.attendance.service.AttendanceService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;

    @PostMapping
    public ApiResponse<Boolean> attend() {
        return attendanceService.attend();
    }

    @GetMapping
    public ApiResponse<List<GetAttendanceResponse>> getMonthlyAttendance(@RequestParam int year, @RequestParam int month) {
        return attendanceService.getMonthlyAttendance(year, month);
    }
}
