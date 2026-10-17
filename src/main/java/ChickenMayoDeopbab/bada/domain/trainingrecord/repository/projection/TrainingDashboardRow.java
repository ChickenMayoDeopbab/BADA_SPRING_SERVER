package ChickenMayoDeopbab.bada.domain.trainingrecord.repository.projection;

import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingSpeechMetrics;

import java.time.LocalDateTime;

/** 대시보드 조회에서 대화 원문과 녹음 정보를 읽지 않기 위한 projection. */
public record TrainingDashboardRow(Long recordId, Long scenarioId, String scenarioName, String scenarioVersion,
                                    String difficulty, Integer scriptLevel, SessionType sessionType,
                                    AiPersonality aiPersonality, EndReason endReason, LocalDateTime startedAt,
                                    Long durationSeconds, Short anxietyScore, TrainingSpeechMetrics speechMetrics) {
    public record FirstTraining(Long scenarioId, SessionType sessionType, LocalDateTime startedAt) {}
}
