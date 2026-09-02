package ChickenMayoDeopbab.bada.domain.adminstatistics.model;

import java.time.LocalDateTime;

public record AppliedTrainingTimelineRow(
        Long userId,
        LocalDateTime scoreAppliedAt
) {
}
