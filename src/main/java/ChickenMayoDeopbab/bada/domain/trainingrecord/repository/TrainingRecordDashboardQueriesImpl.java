package ChickenMayoDeopbab.bada.domain.trainingrecord.repository;

import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request.TrainingRecordFilter;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.projection.TrainingDashboardRow;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static ChickenMayoDeopbab.bada.domain.trainingrecord.entity.QTrainingRecord.trainingRecord;

@RequiredArgsConstructor
public class TrainingRecordDashboardQueriesImpl implements TrainingRecordDashboardQueries {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<TrainingDashboardRow> findDashboardRows(Users user, LocalDateTime from, LocalDateTime until,
                                                       TrainingRecordFilter filter) {
        var record = trainingRecord;
        return queryFactory.select(Projections.constructor(TrainingDashboardRow.class,
                        record.recordId, record.scenarioId, record.scenarioName, record.scenarioVersion,
                        record.difficulty, record.scriptLevel, record.sessionType, record.aiPersonality,
                        record.endReason, record.startedAt, record.durationSeconds, record.anxietyScore,
                        record.speechMetrics))
                .from(record).where(conditions(user, from, until, filter))
                .orderBy(record.startedAt.asc(), record.recordId.asc()).fetch();
    }

    @Override
    public Page<TrainingRecord> findFilteredTrainingRecords(Users user, LocalDateTime from, LocalDateTime until,
                                                           TrainingRecordFilter filter, Pageable pageable) {
        var record = trainingRecord;
        var where = conditions(user, from, until, filter);
        var rows = queryFactory.selectFrom(record).where(where)
                .orderBy(record.startedAt.desc(), record.recordId.desc())
                .offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();
        Long total = queryFactory.select(record.count()).from(record).where(where).fetchOne();
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    private BooleanBuilder conditions(Users user, LocalDateTime from, LocalDateTime until, TrainingRecordFilter filter) {
        var record = trainingRecord;
        BooleanBuilder where = new BooleanBuilder(record.user.eq(user));
        if (from != null) where.and(record.startedAt.goe(from));
        if (until != null) where.and(record.startedAt.lt(until));
        if (filter.sessionType() != null) where.and(record.sessionType.eq(filter.sessionType()));
        if (filter.scenarioId() != null) where.and(record.scenarioId.eq(filter.scenarioId()));
        if (filter.scenarioVersion() != null) where.and(record.scenarioVersion.eq(filter.scenarioVersion()));
        if (filter.difficulty() != null) where.and(record.difficulty.eq(filter.difficulty()));
        if (filter.aiPersonality() != null) where.and(record.aiPersonality.eq(filter.aiPersonality()));
        if (filter.scriptLevel() != null) where.and(record.scriptLevel.eq(filter.scriptLevel()));
        return where;
    }

    @Override
    public List<TrainingDashboardRow.FirstTraining> findFirstScenarioTrainings(
            Users user, Collection<Long> scenarioIds, Collection<EndReason> activityEndReasons) {
        if (scenarioIds.isEmpty()) return List.of();
        var record = trainingRecord;
        return queryFactory.select(Projections.constructor(TrainingDashboardRow.FirstTraining.class,
                        record.scenarioId, record.sessionType, record.startedAt.min()))
                .from(record).where(record.user.eq(user), record.scenarioId.in(scenarioIds),
                        record.sessionType.in(SessionType.SCENARIO, SessionType.CUSTOM),
                        record.endReason.in(activityEndReasons))
                .groupBy(record.scenarioId, record.sessionType).fetch();
    }
}
