package ChickenMayoDeopbab.bada.domain.trainingcallschedule.port;

import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallSchedule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingPushNotificationAdapter implements PushNotificationPort {

    @Override
    public void notifyIncomingCall(TrainingCallSchedule schedule) {
        log.info(
                "AI 발신 이벤트 생성 scheduleId={} userId={} scenarioId={} sessionId={} scheduledAt={}",
                schedule.getScheduleId(),
                schedule.getUser().getUserId(),
                schedule.getScenarioId(),
                schedule.getSessionId(),
                schedule.getScheduledAt()
        );
    }
}
