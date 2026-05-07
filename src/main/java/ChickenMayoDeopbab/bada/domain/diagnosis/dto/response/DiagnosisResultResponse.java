package ChickenMayoDeopbab.bada.domain.diagnosis.dto.response;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DiagnosisResultResponse {
    private double score;
    private String levelName;
    private String levelDescription;
    private String summary;

    public static DiagnosisResultResponse of(double score, CallPhobiaLevel level, String summary) {
        return DiagnosisResultResponse.builder()
                .score(score)
                .levelName(level.getName())
                .levelDescription(level.getDescription())
                .summary(summary)
                .build();
    }
}