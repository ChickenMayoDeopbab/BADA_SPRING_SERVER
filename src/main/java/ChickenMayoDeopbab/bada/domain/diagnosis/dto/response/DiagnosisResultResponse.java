package ChickenMayoDeopbab.bada.domain.diagnosis.dto.response;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DiagnosisResultResponse {
    private double score;
    private CallPhobiaLevel level;
    private String summary;

    public static DiagnosisResultResponse of(double score, CallPhobiaLevel level, String summary) {
        return DiagnosisResultResponse.builder()
                .score(score)
                .level(level)
                .summary(summary)
                .build();
    }
}