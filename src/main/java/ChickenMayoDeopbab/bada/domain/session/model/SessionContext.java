package ChickenMayoDeopbab.bada.domain.session.model;

import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;

public record SessionContext(
        Long userId,
        SessionType type,
        AiPersonality aiPersonality,
        Integer maxDurationSeconds,
        ScenarioContext scenario
) {}