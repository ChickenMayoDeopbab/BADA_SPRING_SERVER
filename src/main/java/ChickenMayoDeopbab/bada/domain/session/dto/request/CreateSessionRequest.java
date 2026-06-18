package ChickenMayoDeopbab.bada.domain.session.dto.request;

import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;

public record CreateSessionRequest(
        Long scenarioId,
        SessionType type,
        AiPersonality aiPersonality,
        String difficulty,
        Integer maxDurationSeconds
) {}