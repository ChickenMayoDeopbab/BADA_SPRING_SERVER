package ChickenMayoDeopbab.bada.domain.session.model;

import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;

import java.time.LocalDateTime;

public record SessionContext(
        Long userId,
        Long scenarioId,
        SessionType type,
        AiPersonality aiPersonality,
        Integer maxDurationSeconds,
        LocalDateTime startedAt,
        ScenarioContext scenario,
        Integer scriptLevel,     // 힌트 레벨 1은 추천 문장, 2는 조언, 3은 없음
        String difficulty
) {}
