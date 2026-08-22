package ChickenMayoDeopbab.bada.domain.attendance.repository;

import ChickenMayoDeopbab.bada.domain.attendance.entity.Attendance;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static ChickenMayoDeopbab.bada.domain.attendance.entity.QAttendance.attendance;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AttendanceQueryRepositoryImpl implements AttendanceQueryRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Attendance> getAttendanceByYearAndMonth(int year, int month, Users userInfo) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        return queryFactory.selectFrom(attendance)
                .where(attendance.user.eq(userInfo))
                .where(attendance.attendedDate.between(start,end))
                .orderBy(attendance.attendedDate.asc())
                .fetch();
    }

    @Override
    public Integer getTotalAttendance(Users userInfo) {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        Long total = queryFactory.select(attendance.count())
                .from(attendance)
                .where(attendance.user.eq(userInfo))
                .where(attendance.attendedDate.between(start,end))
                .fetchOne();

        return total == null ? 0 : total.intValue();
    }
}
