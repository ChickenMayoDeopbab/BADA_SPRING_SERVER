package ChickenMayoDeopbab.bada.domain.diagnosis.dto.response;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisResult;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DiagnosisResultResponse {
    private double score;
    private String levelName;
    private String levelDescription;
    private String summary;
    private Long diagnosisResultId;
    private String questionnaireVersion;

    public static DiagnosisResultResponse from(DiagnosisResult result) {
        return DiagnosisResultResponse.builder().score(result.getScore()).levelName(result.getLevel().getName())
                .levelDescription(result.getLevel().getDescription()).summary(result.getSummary())
                .diagnosisResultId(result.getQuestionId()).questionnaireVersion(result.getQuestionnaireVersion()).build();
    }

    public static DiagnosisResultResponse of(double score, CallPhobiaLevel level, String summary) {
        return DiagnosisResultResponse.builder()
                .score(score)
                .levelName(level.getName())
                .levelDescription(level.getDescription())
                .summary(summary)
                .build();
    }
}
