package ChickenMayoDeopbab.bada.domain.session.port;

import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;
import ChickenMayoDeopbab.bada.domain.session.model.SessionContext;
import ChickenMayoDeopbab.bada.domain.session.model.TranscriptTurn;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class TrainingRecordSessionRecordAdapter implements SessionRecordPort {

    private static final String UNKNOWN_SCENARIO_NAME = "알 수 없음";

    private final UsersRepository usersRepository;
    private final TrainingRecordRepository trainingRecordRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void record(
            String sessionId,
            SessionContext context,
            EndReason reason,
            List<TranscriptTurn> transcript,
            Double silenceTotal,
            Integer shakeCount,
            List<GoodSegment> goodSegments,
            String recordingUrl
    ) {
        if (trainingRecordRepository.existsBySessionId(sessionId)) {
            log.info("중복 세션 종료 기록 무시 sessionId={}", sessionId);
            return;
        }

        Users user = usersRepository.findById(context.userId())
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));

        LocalDateTime endedAt = LocalDateTime.now();
        LocalDateTime startedAt = context.startedAt() != null ? context.startedAt() : endedAt;

        TrainingRecord record = TrainingRecord.builder()
                .user(user)
                .sessionId(sessionId)
                .scenarioId(context.scenarioId())
                .scenarioName(resolveScenarioName(context))
                .sessionType(context.type())
                .aiPersonality(context.aiPersonality())
                .endReason(reason)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .durationSeconds(Duration.between(startedAt, endedAt).toSeconds())
                .transcript(toJson(transcript))
                .goodSegments(toJson(goodSegments))
                .recordingUrl(recordingUrl)
                .build();

        trainingRecordRepository.save(record);
    }

    private String resolveScenarioName(SessionContext context) {
        if (context.scenario() == null || context.scenario().title() == null) {
            return UNKNOWN_SCENARIO_NAME;
        }
        return context.scenario().title();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("훈련 기록 JSON 변환에 실패했습니다.", e);
        }
    }
}
