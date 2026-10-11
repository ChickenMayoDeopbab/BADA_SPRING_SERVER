package ChickenMayoDeopbab.bada.domain.diagnosis.dto.request;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisType;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DiagnosisSubmitRequest {
    @NotBlank
    private String sessionId;

    @NotNull
    private DiagnosisType type;

    private List<@NotNull @Min(1) @Max(5) Integer> answers;

    private String questionnaireVersion;

    @Valid
    private List<QuestionAnswer> answersByQuestionId;

    @AssertTrue(message = "응답 형식을 하나만 선택해야 합니다.")
    public boolean isExactlyOneAnswerFormat() {
        return (answers != null && !answers.isEmpty()) !=
                (answersByQuestionId != null && !answersByQuestionId.isEmpty());
    }

    public record QuestionAnswer(@NotNull Long questionId, @NotNull @Min(1) @Max(5) Integer value) {}
}
