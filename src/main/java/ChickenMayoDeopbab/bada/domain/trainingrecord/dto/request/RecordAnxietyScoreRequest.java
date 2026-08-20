package ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RecordAnxietyScoreRequest(
        @NotNull
        @Min(value = 0, message = "불안 점수는 0이상이어야 합니다")
        @Max(value = 10, message = "불안 점수는 10이하여야 합니다")
        Short score
) {
}
