package ChickenMayoDeopbab.bada.domain.trainingcallschedule.dto.request;

import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateTrainingCallScheduleRequest(
        @NotNull
        Long scenarioId,

        @NotNull
        SessionType type,

        @NotNull
        AiPersonality aiPersonality,

        @NotNull
        @Min(0)
        Integer minDelayMinutes,

        @NotNull
        @Min(0)
        Integer maxDelayMinutes,

        @Positive
        Integer maxDurationSeconds
) {
}
