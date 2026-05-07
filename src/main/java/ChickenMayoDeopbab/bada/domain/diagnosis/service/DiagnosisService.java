package ChickenMayoDeopbab.bada.domain.diagnosis.service;

import ChickenMayoDeopbab.bada.domain.diagnosis.dto.request.DiagnosisSubmitRequest;
import ChickenMayoDeopbab.bada.domain.diagnosis.dto.response.DiagnosisQuestionResponse;
import ChickenMayoDeopbab.bada.domain.diagnosis.dto.response.DiagnosisResultResponse;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisQuestion;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisResult;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisType;
import ChickenMayoDeopbab.bada.domain.diagnosis.repository.DiagnosisRepository;
import ChickenMayoDeopbab.bada.domain.diagnosis.repository.DiagnosisResultRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class DiagnosisService {
    private final DiagnosisRepository diagnosisRepository;
    private final DiagnosisResultRepository diagnosisResultRepository;
    private final UsersRepository usersRepository;
    private final DiagnosisAiService diagnosisAiService;

    @Transactional(readOnly = true)
    public List<DiagnosisQuestionResponse> getQuestions(DiagnosisType type) {
        return diagnosisRepository.findByTypeOrderByOrderIndex(type)
                .stream()
                .map(DiagnosisQuestionResponse::from)
                .toList();
    }

    @Transactional
    public DiagnosisResultResponse submitAnswers(DiagnosisSubmitRequest request) {
        double score = calculateScore(request.getAnswers());
        CallPhobiaLevel level = calculateLevel(score);

        List<DiagnosisQuestion> questions = diagnosisRepository.findByTypeOrderByOrderIndex(request.getType());

        String summary = diagnosisAiService.generateSummary(questions, request.getAnswers());

        Users user = null;
        if (request.getUserId() != null) {
            user = usersRepository.findById(request.getUserId())
                    .orElse(null);
        }
        DiagnosisResult result = DiagnosisResult.builder()
                .user(user)
                .sessionId(request.getSessionId())
                .type(request.getType())
                .score(score)
                .level(level)
                .summary(summary)
                .build();
        diagnosisResultRepository.save(result);
        return DiagnosisResultResponse.of(score, level, summary);
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
}
