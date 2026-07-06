package ChickenMayoDeopbab.bada.domain.trainingcallschedule.dto.response;

import ChickenMayoDeopbab.bada.domain.session.dto.response.CreateSessionResponse;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallSchedule;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallScheduleStatus;

public record AcceptTrainingCallScheduleResponse(
        Long scheduleId,
        TrainingCallScheduleStatus status,
        String sessionId,
        String wsUrl
) {
    public static AcceptTrainingCallScheduleResponse of(
            TrainingCallSchedule schedule,
            CreateSessionResponse session
    ) {
        return new AcceptTrainingCallScheduleResponse(
                schedule.getScheduleId(),
                schedule.getStatus(),
                session.sessionId(),
                session.wsUrl()
        );
    }
}
