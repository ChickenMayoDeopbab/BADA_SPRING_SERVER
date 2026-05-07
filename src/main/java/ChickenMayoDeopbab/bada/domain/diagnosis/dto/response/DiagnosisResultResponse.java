package ChickenMayoDeopbab.bada.domain.diagnosis.dto.response;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DiagnosisResultResponse {
    private double score;
    private CallPhobiaLevel level;

    public static DiagnosisResultResponse of(double score, CallPhobiaLevel level) {
        return DiagnosisResultResponse.builder()
                .score(score)
                .level(level)
                .build();
    }
}