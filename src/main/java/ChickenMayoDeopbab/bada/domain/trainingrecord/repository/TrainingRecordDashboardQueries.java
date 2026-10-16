package ChickenMayoDeopbab.bada.domain.trainingrecord.repository;

import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request.TrainingRecordFilter;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.projection.TrainingDashboardRow;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TrainingRecordDashboardQueries {
    List<TrainingDashboardRow> findDashboardRows(Users user, LocalDateTime from, LocalDateTime until,
                                                TrainingRecordFilter filter);

    List<TrainingDashboardRow.FirstTraining> findFirstScenarioTrainings(
            Users user, Collection<Long> scenarioIds, Collection<EndReason> activityEndReasons);
}
