package ChickenMayoDeopbab.bada.domain.dashboard.service;

import ChickenMayoDeopbab.bada.domain.dashboard.dto.request.DashboardQuery;
import ChickenMayoDeopbab.bada.domain.dashboard.dto.request.DashboardQuery.DateRange;
import ChickenMayoDeopbab.bada.domain.dashboard.dto.request.DashboardQuery.Resolved;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.projection.TrainingDashboardRow;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ChickenMayoDeopbab.bada.domain.dashboard.dto.response.DashboardAnalyticsResponse.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardAnalyticsService {
    private final TrainingRecordRepository records;
    private final UsersRepository users;
    private final DashboardAggregator aggregator;
    private final Clock dashboardClock;

    public Overview overview(DashboardQuery request) {
        Clock clock = requestClock();
        Resolved query = request.resolve(clock, false, false);
        Users user = currentUser();
        var rows = find(user, query.period(), query);
        var training = aggregator.trainings(rows);
        var first = firstTrainings(user, training);
        Set<ScenarioKey> keys = training.stream().filter(row -> row.scenarioId() != null)
                .map(this::scenarioKey).collect(Collectors.toSet());
        long newScenarios = keys.stream().filter(key -> first.containsKey(key)
                && query.period().contains(first.get(key))).count();
        var metrics = aggregator.metrics(rows);
        Map<String, Long> counts = new LinkedHashMap<>();
        metrics.forEach((key, value) -> counts.put(key, value.sampleCount()));
        return new Overview(meta(query, clock), aggregator.summary(rows),
                new Experience(keys.size(), newScenarios, training.stream().filter(row -> row.scenarioId() == null).count()),
                aggregator.hintSettings(rows), metrics,
                aggregator.series(rows, query.period(), query.interval(), LocalDate.now(clock)),
                aggregator.activity(rows, query.period()),
                new Coverage(training.size(), metrics.get(DashboardAggregator.Metric.ANXIETY.key).sampleCount(), counts));
    }

    public Comparison comparison(DashboardQuery request) {
        Clock clock = requestClock();
        Resolved query = request.resolve(clock, true, false);
        Users user = currentUser();
        var baseline = find(user, query.baseline(), query);
        var current = find(user, query.period(), query);
        var comparison = aggregator.compare(baseline, current);
        return new Comparison(meta(query, clock), aggregator.summary(baseline), aggregator.summary(current),
                comparison.metrics(), aggregator.conditions(baseline), aggregator.conditions(current),
                aggregator.comparisonSeries(baseline, query.baseline(), query.interval(), LocalDate.now(clock), comparison.baselineIds()),
                aggregator.comparisonSeries(current, query.period(), query.interval(), LocalDate.now(clock), comparison.currentIds()));
    }

    public Scenarios scenarios(DashboardQuery request) {
        Clock clock = requestClock();
        Resolved query = request.resolve(clock, true, true);
        Users user = currentUser();
        var current = aggregator.trainings(find(user, query.period(), query));
        Map<ScenarioKey, List<TrainingDashboardRow>> groups = scenarioGroups(current);
        List<ScenarioKey> keys = new ArrayList<>(groups.keySet());
        keys.sort(Comparator.<ScenarioKey, LocalDateTime>comparing(key -> latest(groups.get(key)).startedAt())
                .reversed().thenComparing(key -> key.sessionType().name()).thenComparing(ScenarioKey::scenarioId));
        int start = (int) Math.min((long) query.page() * query.size(), keys.size());
        List<ScenarioKey> selected = keys.subList(start, Math.min(start + query.size(), keys.size()));
        if (selected.isEmpty()) return new Scenarios(meta(query, clock), List.of(), query.page(), query.size(),
                keys.size(), (int) ((keys.size() + (long) query.size() - 1) / query.size()));
        var baseline = scenarioGroups(aggregator.trainings(find(user, query.baseline(), query)));
        var first = firstTrainings(user, selected.stream().flatMap(key -> groups.get(key).stream()).toList());
        List<Scenario> items = selected.stream().map(key -> {
            var rows = groups.get(key);
            var last = latest(rows);
            return new Scenario(key.scenarioId(), key.sessionType(), last.scenarioName(), aggregator.summary(rows),
                    first.get(key), last.startedAt(), aggregator.conditions(rows), aggregator.metrics(rows),
                    aggregator.compare(baseline.getOrDefault(key, List.of()), rows).metrics(),
                    query.includeSeries() ? aggregator.series(rows, query.period(), query.interval(), LocalDate.now(clock)) : List.of());
        }).toList();
        return new Scenarios(meta(query, clock), items, query.page(), query.size(), keys.size(),
                (int) ((keys.size() + (long) query.size() - 1) / query.size()));
    }

    private List<TrainingDashboardRow> find(Users user, DateRange range, Resolved query) {
        return records.findDashboardRows(user, range.from().atStartOfDay(), range.to().plusDays(1).atStartOfDay(), query.filters());
    }

    private Map<ScenarioKey, LocalDateTime> firstTrainings(Users user, List<TrainingDashboardRow> rows) {
        Set<Long> ids = rows.stream().map(TrainingDashboardRow::scenarioId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        Map<ScenarioKey, LocalDateTime> result = new HashMap<>();
        records.findFirstScenarioTrainings(user, ids, DashboardAggregator.ACTIVITY_END_REASONS)
                .forEach(first -> result.put(new ScenarioKey(first.sessionType(), first.scenarioId()), first.startedAt()));
        return result;
    }

    private Map<ScenarioKey, List<TrainingDashboardRow>> scenarioGroups(List<TrainingDashboardRow> rows) {
        return rows.stream().filter(row -> row.scenarioId() != null)
                .collect(Collectors.groupingBy(this::scenarioKey, LinkedHashMap::new, Collectors.toList()));
    }

    private TrainingDashboardRow latest(List<TrainingDashboardRow> rows) {
        return rows.stream().max(Comparator.comparing(TrainingDashboardRow::startedAt)
                .thenComparing(TrainingDashboardRow::recordId)).orElseThrow();
    }

    private ScenarioKey scenarioKey(TrainingDashboardRow row) { return new ScenarioKey(row.sessionType(), row.scenarioId()); }

    private Meta meta(Resolved query, Clock clock) {
        LocalDate today = LocalDate.now(clock);
        return new Meta(period(query.period(), today), query.baseline() == null ? null : period(query.baseline(), today),
                query.interval(), query.filters(), clock.getZone().getId(), Instant.now(clock), DashboardAggregator.POLICY_VERSION);
    }

    private Period period(DateRange range, LocalDate today) {
        return new Period(range.from(), range.to(), !range.to().isBefore(today));
    }

    private Clock requestClock() {
        // 조회 도중 자정이 지나도 기본 기간과 부분 구간 표시가 서로 어긋나지 않는다.
        return Clock.fixed(dashboardClock.instant(), dashboardClock.getZone());
    }

    private Users currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ApplicationException(UsersStatusCode.USER_NOT_FOUND);
        }
        return users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));
    }

    private record ScenarioKey(SessionType sessionType, Long scenarioId) {}
}
