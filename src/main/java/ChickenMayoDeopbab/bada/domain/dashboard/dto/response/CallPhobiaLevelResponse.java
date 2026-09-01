package ChickenMayoDeopbab.bada.domain.dashboard.dto.response;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;

public record CallPhobiaLevelResponse(
        String code,
        String name
) {
    public static CallPhobiaLevelResponse from(
            CallPhobiaLevel level
    ) {
        if (level == null) {
            return null;
        }

        return new CallPhobiaLevelResponse(
                level.name(),
                level.getName()
        );
    }
}
