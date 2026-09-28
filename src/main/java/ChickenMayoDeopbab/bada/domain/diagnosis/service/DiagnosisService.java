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
        return diagnosisRepository.findByTypeOrderByOrderIndex(type)
                .stream()
                .map(DiagnosisQuestionResponse::from)
                .toList();
    }

    @Transactional
    public DiagnosisResultResponse submitAnswers(DiagnosisSubmitRequest request) {
        Users user = getCurrentUser();
        sensitiveInformationConsentPolicy.ensureAgreed(user);

        double score = calculateScore(request.getAnswers());
        CallPhobiaLevel level = calculateLevel(score);

        List<DiagnosisQuestion> questions = diagnosisRepository.findByTypeOrderByOrderIndex(request.getType());

        String summary = diagnosisAiService.generateSummary(questions, request.getAnswers());

        DiagnosisResult result = DiagnosisResult.builder()
                .user(user)
                .sessionId(request.getSessionId())
                .type(request.getType())
                .score(score)
                .level(level)
                .summary(summary)
                .build();
        diagnosisResultRepository.save(result);

        initializeCallAnxietyState(user, score, level);
        return DiagnosisResultResponse.of(score, level, summary);
    }

    private Users getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return usersRepository.findByUsername(authentication.getName())
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
