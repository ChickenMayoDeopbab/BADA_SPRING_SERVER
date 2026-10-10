package ChickenMayoDeopbab.bada.domain.diagnosis.service;

import ChickenMayoDeopbab.bada.domain.callanxiety.repository.CallAnxietyStateRepository;
import ChickenMayoDeopbab.bada.domain.diagnosis.dto.request.DiagnosisSubmitRequest;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisQuestion;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisType;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisResult;
import ChickenMayoDeopbab.bada.domain.diagnosis.repository.DiagnosisRepository;
import ChickenMayoDeopbab.bada.domain.diagnosis.repository.DiagnosisResultRepository;
import ChickenMayoDeopbab.bada.domain.legalconsent.service.SensitiveInformationConsentPolicy;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DiagnosisServiceTest {
    private final DiagnosisRepository questions = mock(DiagnosisRepository.class);
    private final DiagnosisResultRepository results = mock(DiagnosisResultRepository.class);
    private final DiagnosisAiService ai = mock(DiagnosisAiService.class);
    private final UsersRepository users = mock(UsersRepository.class);
    private final DiagnosisService service = new DiagnosisService(questions, results, users, ai,
            mock(CallAnxietyStateRepository.class), mock(SensitiveInformationConsentPolicy.class));

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void rejectsWrongAnswerCountBeforeCallingAiOrSaving() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", null, List.of()));
        when(users.findByUsernameForDiagnosisUpdate("tester")).thenReturn(Optional.of(mock(Users.class)));
        when(questions.findByTypeOrderByOrderIndex(DiagnosisType.SIGNUP))
                .thenReturn(List.of(new DiagnosisQuestion(), new DiagnosisQuestion()));
        DiagnosisSubmitRequest request = new DiagnosisSubmitRequest();
        request.setType(DiagnosisType.SIGNUP);
        request.setAnswers(List.of(5, 5, 1, 1));

        assertThatThrownBy(() -> service.submitAnswers(request)).isInstanceOf(ApplicationException.class);
        verifyNoInteractions(ai, results);
    }

    @Test
    void retriesReuseSavedResultAndDifferentAnswersConflict() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", null, List.of()));
        Users user = mock(Users.class);
        when(users.findByUsernameForDiagnosisUpdate("tester")).thenReturn(Optional.of(user));
        List<DiagnosisQuestion> items = List.of(new DiagnosisQuestion(1L, "전화 불안", DiagnosisType.SIGNUP, 1),
                new DiagnosisQuestion(2L, "전화 회피", DiagnosisType.SIGNUP, 2));
        when(questions.findByTypeOrderByOrderIndex(DiagnosisType.SIGNUP)).thenReturn(items);
        AtomicReference<DiagnosisResult> stored = new AtomicReference<>();
        when(results.findFirstByUserAndSessionIdOrderByCreatedAtAsc(user, "same-session"))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(results.save(any())).thenAnswer(invocation -> {
            DiagnosisResult result = invocation.getArgument(0); ReflectionTestUtils.setField(result, "questionId", 42L);
            stored.set(result); return result;
        });
        when(ai.generateSummary(anyList(), anyList())).thenReturn("요약");
        DiagnosisSubmitRequest request = new DiagnosisSubmitRequest(); request.setType(DiagnosisType.SIGNUP);
        request.setSessionId("same-session"); request.setAnswers(List.of(4, 3));
        request.setQuestionnaireVersion(DiagnosisQuestionnaire.version(DiagnosisType.SIGNUP, items));
        assertThat(service.submitAnswers(request).getDiagnosisResultId()).isEqualTo(42L);
        assertThat(service.submitAnswers(request).getDiagnosisResultId()).isEqualTo(42L);
        assertThat(stored.get().getAnswerSnapshot()).extracting(DiagnosisResult.AnswerSnapshot::value).containsExactly(4, 3);
        assertThat(stored.get().getQuestionnaireVersionConfirmed()).isTrue();
        request.setAnswers(List.of(1, 1));
        assertThatThrownBy(() -> service.submitAnswers(request)).isInstanceOf(ApplicationException.class);
        verify(ai, times(1)).generateSummary(anyList(), anyList()); verify(results, times(1)).save(any());
    }
}
