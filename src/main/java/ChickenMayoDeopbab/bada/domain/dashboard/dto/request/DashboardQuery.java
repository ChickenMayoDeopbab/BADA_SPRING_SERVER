package ChickenMayoDeopbab.bada.domain.dashboard.dto.request;

import ChickenMayoDeopbab.bada.domain.dashboard.exception.DashboardStatusCode;
import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request.TrainingRecordFilter;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record DashboardQuery(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        Interval interval, SessionType sessionType, Long scenarioId, String scenarioVersion,
        String difficulty, AiPersonality aiPersonality, Integer scriptLevel,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baselineFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baselineTo,
        Integer page, Integer size, Boolean includeSeries
) {
    public enum Interval { DAY, WEEK }

    public Resolved resolve(Clock clock, boolean comparison, boolean scenarios) {
        LocalDate today = LocalDate.now(clock);
        require((from == null) == (to == null), "from과 to는 함께 지정해야 합니다.");
        DateRange period = new DateRange(from == null ? today.minusDays(27) : from, to == null ? today : to);
        validateRange(period, today);
        require(sessionType != SessionType.WARMUP, "워밍업은 시나리오 성장 필터에 사용할 수 없습니다.");
        require(scenarioId == null || scenarioId > 0, "scenarioId는 양수여야 합니다.");
        require(scriptLevel == null || (scriptLevel >= 1 && scriptLevel <= 3), "scriptLevel은 1~3이어야 합니다.");
        require(scenarioVersion == null || (!scenarioVersion.isBlank() && scenarioVersion.length() <= 255),
                "scenarioVersion이 올바르지 않습니다.");
        require(difficulty == null || (!difficulty.isBlank() && difficulty.length() <= 255),
                "difficulty가 올바르지 않습니다.");
        require(comparison || (baselineFrom == null && baselineTo == null), "이 조회는 비교 기간을 받지 않습니다.");
        DateRange baseline = null;
        if (comparison) {
            require((baselineFrom == null) == (baselineTo == null), "기준 기간의 시작과 끝을 함께 지정해야 합니다.");
            long days = ChronoUnit.DAYS.between(period.from(), period.to()) + 1;
            baseline = baselineFrom == null
                    ? new DateRange(period.from().minusDays(days), period.from().minusDays(1))
                    : new DateRange(baselineFrom, baselineTo);
            validateRange(baseline, today);
            require(baseline.to().isBefore(period.from()), "기준 기간은 조회 기간보다 앞서야 합니다.");
            require(ChronoUnit.DAYS.between(baseline.from(), baseline.to()) + 1 == days,
                    "두 비교 기간의 일수가 같아야 합니다.");
        }
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? 20 : size;
        require(resolvedPage >= 0 && resolvedSize >= 1 && resolvedSize <= 50, "page는 0 이상, size는 1~50이어야 합니다.");
        require(scenarios || (page == null && size == null && includeSeries == null), "목록 옵션은 상황별 조회에만 사용할 수 있습니다.");
        require(!Boolean.TRUE.equals(includeSeries) || scenarioId != null, "시계열을 포함하려면 scenarioId가 필요합니다.");
        return new Resolved(period, baseline, interval == null ? Interval.WEEK : interval,
                new TrainingRecordFilter(sessionType, scenarioId, scenarioVersion, difficulty, aiPersonality, scriptLevel),
                resolvedPage, resolvedSize, Boolean.TRUE.equals(includeSeries));
    }

    private static void validateRange(DateRange range, LocalDate today) {
        require(!range.from().isBefore(LocalDate.of(2000, 1, 1)) && !range.to().isAfter(today)
                        && !range.to().isBefore(range.from())
                        && ChronoUnit.DAYS.between(range.from(), range.to()) < 366,
                "조회 기간은 2000년 이후부터 오늘까지, 최대 366일이어야 합니다.");
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new ApplicationException(DashboardStatusCode.INVALID_QUERY, message);
    }

    public record DateRange(LocalDate from, LocalDate to) {
        public boolean contains(LocalDateTime value) {
            return !value.toLocalDate().isBefore(from) && !value.toLocalDate().isAfter(to);
        }
    }

    public record Resolved(DateRange period, DateRange baseline, Interval interval, TrainingRecordFilter filters,
                           int page, int size, boolean includeSeries) {}
}
