package ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request;

import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;

public record TrainingRecordFilter(SessionType sessionType, Long scenarioId, String scenarioVersion,
                                   String difficulty, AiPersonality aiPersonality, Integer scriptLevel) {
}
