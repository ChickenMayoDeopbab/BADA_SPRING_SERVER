package ChickenMayoDeopbab.bada.domain.diagnosis.service;

import ChickenMayoDeopbab.bada.domain.callanxiety.entity.CallAnxietyState;
import ChickenMayoDeopbab.bada.domain.callanxiety.repository.CallAnxietyStateRepository;
import ChickenMayoDeopbab.bada.domain.diagnosis.dto.request.DiagnosisSubmitRequest;
import ChickenMayoDeopbab.bada.domain.diagnosis.dto.response.DiagnosisQuestionResponse;
import ChickenMayoDeopbab.bada.domain.diagnosis.dto.response.DiagnosisResultResponse;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisQuestion;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisResult;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisType;
import ChickenMayoDeopbab.bada.domain.diagnosis.exception.DiagnosisResultStatusCode;
import ChickenMayoDeopbab.bada.domain.diagnosis.repository.DiagnosisRepository;
import ChickenMayoDeopbab.bada.domain.diagnosis.repository.DiagnosisResultRepository;
import ChickenMayoDeopbab.bada.domain.legalconsent.service.SensitiveInformationConsentPolicy;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Service
public class DiagnosisService {
    private final DiagnosisRepository diagnosisRepository;
    private final DiagnosisResultRepository diagnosisResultRepository;
    private final UsersRepository usersRepository;
    private final DiagnosisAiService diagnosisAiService;
    private final CallAnxietyStateRepository callAnxietyStateRepository;
    private final SensitiveInformationConsentPolicy sensitiveInformationConsentPolicy;

    @Transactional(readOnly = true)
    public List<DiagnosisQuestionResponse> getQuestions(DiagnosisType type) {
        List<DiagnosisQuestion> questions = diagnosisRepository.findByTypeOrderByOrderIndex(type);
        String version = DiagnosisQuestionnaire.version(type, questions);
        return questions
                .stream()
                .map(question -> DiagnosisQuestionResponse.from(question, version))
                .toList();
    }

    @Transactional
    public DiagnosisResultResponse submitAnswers(DiagnosisSubmitRequest request) {
        Users user = getCurrentUser();
        sensitiveInformationConsentPolicy.ensureAgreed(user);

        List<DiagnosisQuestion> questions = diagnosisRepository.findByTypeOrderByOrderIndex(request.getType());
        List<Integer> answers = DiagnosisQuestionnaire.answers(request, questions);
        DiagnosisResult existing = diagnosisResultRepository.findFirstByUserAndSessionIdOrderByCreatedAtAsc(user, request.getSessionId()).orElse(null);
        if (existing != null) {
            if (existing.getType() != request.getType() || existing.getAnswerSnapshot() == null
                    || !existing.getAnswerSnapshot().stream().map(DiagnosisResult.AnswerSnapshot::value).toList().equals(answers)
                    || !DiagnosisQuestionnaire.version(request.getType(), questions).equals(existing.getQuestionnaireVersion())) {
                throw new ApplicationException(DiagnosisResultStatusCode.DIAGNOSIS_SESSION_CONFLICT);
            }
            return DiagnosisResultResponse.from(existing);
        }

        double score = calculateScore(answers);
        CallPhobiaLevel level = calculateLevel(score);

        String summary = diagnosisAiService.generateSummary(questions, answers);

        DiagnosisResult result = DiagnosisResult.builder()
                .user(user)
                .sessionId(request.getSessionId())
                .type(request.getType())
                .score(score)
                .level(level)
                .summary(summary)
                .questionnaireVersion(DiagnosisQuestionnaire.version(request.getType(), questions))
                .questionnaireVersionConfirmed(request.getQuestionnaireVersion() != null)
                .answerSnapshot(IntStream.range(0, questions.size()).mapToObj(index -> {
                    DiagnosisQuestion question = questions.get(index);
                    return new DiagnosisResult.AnswerSnapshot(question.getQuestionId(), question.getContent(),
                            question.getOrderIndex(), answers.get(index));
                }).toList())
                .build();
        diagnosisResultRepository.save(result);

        initializeCallAnxietyState(user, score, level);
        return DiagnosisResultResponse.from(result);
    }

    private Users getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return usersRepository.findByUsernameForDiagnosisUpdate(authentication.getName())
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));
    }

    private double calculateScore(List<Integer> answers) {
        return answers.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }
    private CallPhobiaLevel calculateLevel(double score) {
        return switch ((int)Math.round(score)) {
            case 1 -> CallPhobiaLevel.LEVEL_1;
            case 2 -> CallPhobiaLevel.LEVEL_2;
            case 3 -> CallPhobiaLevel.LEVEL_3;
            case 4 -> CallPhobiaLevel.LEVEL_4;
            case 5 -> CallPhobiaLevel.LEVEL_5;
            default -> CallPhobiaLevel.LEVEL_1;
        };
    }

    private void initializeCallAnxietyState(
            Users user,
            double score,
            CallPhobiaLevel level
    ) {
        if (callAnxietyStateRepository.existsByUser(user)) {
            return;
        }

        CallAnxietyState state = CallAnxietyState.create(
                user,
                BigDecimal.valueOf(score),
                level,
                LocalDateTime.now()
        );

        callAnxietyStateRepository.save(state);
    }
}
