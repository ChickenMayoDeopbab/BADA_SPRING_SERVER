package ChickenMayoDeopbab.bada.domain.trainingcallschedule.service;

import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallSchedule;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallScheduleStatus;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.port.PushNotificationPort;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.repository.TrainingCallScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingCallScheduleTriggerService {

    private final TrainingCallScheduleRepository trainingCallScheduleRepository;
    private final PushNotificationPort pushNotificationPort;

    @Scheduled(fixedDelayString = "${app.training-call-schedule.trigger-fixed-delay-ms:30000}")
    public void triggerDueSchedules() {
        int triggeredCount = triggerDueSchedules(LocalDateTime.now());
        if (triggeredCount > 0) {
            log.info("AI 발신 예약 트리거 완료 count={}", triggeredCount);
        }
    }

    @Transactional
    public int triggerDueSchedules(LocalDateTime now) {
        List<TrainingCallSchedule> schedules =
                trainingCallScheduleRepository.findTop100ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                        TrainingCallScheduleStatus.SCHEDULED,
                        now
                );

        for (TrainingCallSchedule schedule : schedules) {
            schedule.markRinging(now);
            pushNotificationPort.notifyIncomingCall(schedule);
        }
        return schedules.size();
    }
}
