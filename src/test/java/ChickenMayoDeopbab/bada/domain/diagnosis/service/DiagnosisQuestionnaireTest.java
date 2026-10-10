package ChickenMayoDeopbab.bada.domain.diagnosis.service;

import ChickenMayoDeopbab.bada.domain.diagnosis.dto.request.DiagnosisSubmitRequest;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class DiagnosisQuestionnaireTest {
    private final List<DiagnosisQuestion> questions = List.of(
            new DiagnosisQuestion(1L, "전화가 불안한가요", DiagnosisType.SIGNUP, 1),
            new DiagnosisQuestion(2L, "전화를 피하나요", DiagnosisType.SIGNUP, 2));

    @Test
    void reordersAnswersByQuestionIdAndRejectsDuplicatesOrUnknownVersion() {
        DiagnosisSubmitRequest request = request();
        request.setAnswersByQuestionId(List.of(new DiagnosisSubmitRequest.QuestionAnswer(2L, 2), new DiagnosisSubmitRequest.QuestionAnswer(1L, 5)));
        assertThat(DiagnosisQuestionnaire.answers(request, questions)).containsExactly(5, 2);
        request.setAnswersByQuestionId(List.of(new DiagnosisSubmitRequest.QuestionAnswer(1L, 5), new DiagnosisSubmitRequest.QuestionAnswer(1L, 2)));
        assertThatThrownBy(() -> DiagnosisQuestionnaire.answers(request, questions)).isInstanceOf(RuntimeException.class);
        request.setAnswersByQuestionId(List.of(new DiagnosisSubmitRequest.QuestionAnswer(1L, 5), new DiagnosisSubmitRequest.QuestionAnswer(2L, 2)));
        request.setQuestionnaireVersion("OLD_VERSION");
        assertThatThrownBy(() -> DiagnosisQuestionnaire.answers(request, questions)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void versionChangesWhenQuestionTextOrOrderChanges() {
        String version = DiagnosisQuestionnaire.version(DiagnosisType.SIGNUP, questions);
        assertThat(DiagnosisQuestionnaire.version(DiagnosisType.SIGNUP, List.of(
                new DiagnosisQuestion(1L, "다른 질문", DiagnosisType.SIGNUP, 1), questions.getLast()))).isNotEqualTo(version);
        assertThat(DiagnosisQuestionnaire.version(DiagnosisType.SIGNUP, questions.reversed())).isNotEqualTo(version);
    }

    @Test
    void keepsLegacyOrderedAnswersButDoesNotAcceptBothFormats() {
        DiagnosisSubmitRequest request = new DiagnosisSubmitRequest(); request.setType(DiagnosisType.SIGNUP); request.setAnswers(List.of(4, 3));
        assertThat(DiagnosisQuestionnaire.answers(request, questions)).containsExactly(4, 3);
        request.setAnswersByQuestionId(List.of(new DiagnosisSubmitRequest.QuestionAnswer(1L, 4), new DiagnosisSubmitRequest.QuestionAnswer(2L, 3)));
        assertThatThrownBy(() -> DiagnosisQuestionnaire.answers(request, questions)).isInstanceOf(RuntimeException.class);
    }

    private DiagnosisSubmitRequest request() {
        DiagnosisSubmitRequest request = new DiagnosisSubmitRequest(); request.setType(DiagnosisType.SIGNUP);
        request.setQuestionnaireVersion(DiagnosisQuestionnaire.version(DiagnosisType.SIGNUP, questions)); return request;
    }
}
