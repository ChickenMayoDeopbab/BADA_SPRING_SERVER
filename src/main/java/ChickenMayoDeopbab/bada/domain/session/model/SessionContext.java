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
        ScenarioContext scenario
) {}
