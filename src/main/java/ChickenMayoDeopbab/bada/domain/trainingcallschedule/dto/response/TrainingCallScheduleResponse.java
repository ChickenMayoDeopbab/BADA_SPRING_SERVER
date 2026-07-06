package ChickenMayoDeopbab.bada.domain.trainingcallschedule.dto.response;

import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallSchedule;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallScheduleStatus;

import java.time.LocalDateTime;

public record TrainingCallScheduleResponse(
        Long scheduleId,
        LocalDateTime scheduledAt,
        TrainingCallScheduleStatus status,
        String sessionId,
        String wsUrl
) {
    public static TrainingCallScheduleResponse from(TrainingCallSchedule schedule) {
        return new TrainingCallScheduleResponse(
                schedule.getScheduleId(),
                schedule.getScheduledAt(),
                schedule.getStatus(),
                schedule.getSessionId(),
                schedule.getWsUrl()
        );
    }
}
