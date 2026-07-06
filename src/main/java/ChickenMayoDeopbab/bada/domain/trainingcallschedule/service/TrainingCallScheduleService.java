package ChickenMayoDeopbab.bada.domain.trainingcallschedule.service;

import ChickenMayoDeopbab.bada.domain.session.dto.request.CreateSessionRequest;
import ChickenMayoDeopbab.bada.domain.session.dto.response.CreateSessionResponse;
import ChickenMayoDeopbab.bada.domain.session.service.SessionService;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.dto.request.CreateTrainingCallScheduleRequest;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.dto.response.AcceptTrainingCallScheduleResponse;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.dto.response.TrainingCallScheduleResponse;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallSchedule;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallScheduleStatus;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.exception.TrainingCallScheduleStatusCode;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.repository.TrainingCallScheduleRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class TrainingCallScheduleService {

    private final UsersRepository usersRepository;
    private final TrainingCallScheduleRepository trainingCallScheduleRepository;
    private final SessionService sessionService;

    @Transactional
    public TrainingCallScheduleResponse create(CreateTrainingCallScheduleRequest request) {
        Users user = getUserInfo();
        validateDelayRange(request.minDelayMinutes(), request.maxDelayMinutes());

        TrainingCallSchedule schedule = TrainingCallSchedule.builder()
                .user(user)
                .scenarioId(request.scenarioId())
                .type(request.type())
                .aiPersonality(request.aiPersonality())
                .minDelayMinutes(request.minDelayMinutes())
                .maxDelayMinutes(request.maxDelayMinutes())
                .maxDurationSeconds(request.maxDurationSeconds())
                .scheduledAt(resolveScheduledAt(
                        LocalDateTime.now(),
                        request.minDelayMinutes(),
                        request.maxDelayMinutes()
                ))
                .build();

        return TrainingCallScheduleResponse.from(trainingCallScheduleRepository.save(schedule));
    }

    @Transactional
    public TrainingCallScheduleResponse cancel(Long scheduleId) {
        Users user = getUserInfo();
        TrainingCallSchedule schedule = trainingCallScheduleRepository.findByScheduleIdAndUser(scheduleId, user)
                .orElseThrow(() -> ApplicationException.of(TrainingCallScheduleStatusCode.SCHEDULE_NOT_FOUND));

        if (schedule.getStatus() == TrainingCallScheduleStatus.ACCEPTED) {
            throw ApplicationException.of(TrainingCallScheduleStatusCode.SCHEDULE_NOT_CANCELABLE);
        }
        if (schedule.getStatus() != TrainingCallScheduleStatus.CANCELED) {
            schedule.cancel();
        }
        return TrainingCallScheduleResponse.from(schedule);
    }

    @Transactional
    public AcceptTrainingCallScheduleResponse accept(Long scheduleId, String accessToken) {
        Users user = getUserInfo();
        TrainingCallSchedule schedule = trainingCallScheduleRepository.findByScheduleIdAndUser(scheduleId, user)
                .orElseThrow(() -> ApplicationException.of(TrainingCallScheduleStatusCode.SCHEDULE_NOT_FOUND));

        if (schedule.getStatus() != TrainingCallScheduleStatus.RINGING) {
            throw ApplicationException.of(TrainingCallScheduleStatusCode.SCHEDULE_NOT_ACCEPTABLE);
        }

        CreateSessionResponse session = sessionService.create(
                new CreateSessionRequest(
                        schedule.getScenarioId(),
                        schedule.getType(),
                        schedule.getAiPersonality(),
                        null,
                        schedule.getMaxDurationSeconds()
                ),
                accessToken
        );

        schedule.accept(session.sessionId(), LocalDateTime.now());
        return AcceptTrainingCallScheduleResponse.of(schedule, session);
    }

    static void validateDelayRange(Integer minDelayMinutes, Integer maxDelayMinutes) {
        if (minDelayMinutes == null || maxDelayMinutes == null || minDelayMinutes > maxDelayMinutes) {
            throw ApplicationException.of(TrainingCallScheduleStatusCode.INVALID_DELAY_RANGE);
        }
    }

    static LocalDateTime resolveScheduledAt(
            LocalDateTime now,
            int minDelayMinutes,
            int maxDelayMinutes
    ) {
        int delayMinutes = minDelayMinutes == maxDelayMinutes
                ? minDelayMinutes
                : ThreadLocalRandom.current().nextInt(minDelayMinutes, maxDelayMinutes + 1);
        return now.plusMinutes(delayMinutes);
    }

    private Users getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return usersRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));
    }
}
