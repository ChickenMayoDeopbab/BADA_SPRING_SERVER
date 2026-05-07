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

    public static DiagnosisQuestionResponse from(DiagnosisQuestion question) {
        return DiagnosisQuestionResponse.builder()
                .questionId(question.getQuestionId())
                .content(question.getContent())
                .orderIndex(question.getOrderIndex())
                .build();
    }
}
