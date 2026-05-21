package ChickenMayoDeopbab.bada.domain.attendance.dto.response;

import ChickenMayoDeopbab.bada.domain.attendance.entity.Attendance;

import java.time.LocalDate;
import java.util.List;

public record GetAttendanceResponse(
        LocalDate date
) {
    private static GetAttendanceResponse of(Attendance attendance) {
        return new GetAttendanceResponse(attendance.getAttendedDate());
    }

    public static List<GetAttendanceResponse> fromList(List<Attendance> attendances) {
        return attendances.stream()
                .map(GetAttendanceResponse::of)
                .toList();
    }
}
