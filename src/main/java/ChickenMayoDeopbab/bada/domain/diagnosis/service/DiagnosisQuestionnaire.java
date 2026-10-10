package ChickenMayoDeopbab.bada.domain.diagnosis.service;

import ChickenMayoDeopbab.bada.domain.diagnosis.dto.request.DiagnosisSubmitRequest;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisQuestion;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisType;
import ChickenMayoDeopbab.bada.domain.diagnosis.exception.DiagnosisResultStatusCode;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DiagnosisQuestionnaire {
    private DiagnosisQuestionnaire() {}

    public static String version(DiagnosisType type, List<DiagnosisQuestion> questions) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(type.name().getBytes(StandardCharsets.UTF_8));
            for (DiagnosisQuestion question : questions) {
                String value = "\n" + question.getQuestionId() + ":" + question.getOrderIndex()
                        + ":" + question.getContent();
                digest.update(value.getBytes(StandardCharsets.UTF_8));
            }
            return type.name() + "_" + HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static List<Integer> answers(DiagnosisSubmitRequest request, List<DiagnosisQuestion> questions) {
        if (questions.isEmpty() || !request.isExactlyOneAnswerFormat()) { throw invalid(); }
        String version = version(request.getType(), questions);
        if (request.getQuestionnaireVersion() != null && !Objects.equals(version, request.getQuestionnaireVersion())) {
            throw invalid();
        }
        List<Integer> answers = request.getAnswers();
        if (request.getAnswersByQuestionId() != null && !request.getAnswersByQuestionId().isEmpty()) {
            if (request.getQuestionnaireVersion() == null) { throw invalid(); }
            Map<Long, Integer> byId = new LinkedHashMap<>();
            for (var answer : request.getAnswersByQuestionId()) {
                if (answer == null || answer.questionId() == null || answer.value() == null
                        || byId.putIfAbsent(answer.questionId(), answer.value()) != null) { throw invalid(); }
            }
            if (byId.size() != questions.size()
                    || questions.stream().anyMatch(question -> !byId.containsKey(question.getQuestionId()))) {
                throw invalid();
            }
            answers = questions.stream().map(question -> byId.get(question.getQuestionId())).toList();
        }
        if (answers == null || answers.size() != questions.size()
                || answers.stream().anyMatch(answer -> answer == null || answer < 1 || answer > 5)) { throw invalid(); }
        return List.copyOf(answers);
    }

    private static ApplicationException invalid() {
        return new ApplicationException(DiagnosisResultStatusCode.INVALID_DIAGNOSIS_ANSWERS);
    }
}
