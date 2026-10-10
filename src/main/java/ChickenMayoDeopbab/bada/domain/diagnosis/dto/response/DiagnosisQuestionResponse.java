package ChickenMayoDeopbab.bada.domain.diagnosis.dto.response;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisQuestion;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DiagnosisQuestionResponse {
    private Long questionId;
    private String content;
    private int orderIndex;
    private String questionnaireVersion;

    public static DiagnosisQuestionResponse from(DiagnosisQuestion question) {
        return from(question, null);
    }

    public static DiagnosisQuestionResponse from(DiagnosisQuestion question, String version) {
        return DiagnosisQuestionResponse.builder()
                .questionId(question.getQuestionId())
                .content(question.getContent())
                .orderIndex(question.getOrderIndex())
                .questionnaireVersion(version)
                .build();
    }
}
