package ChickenMayoDeopbab.bada.domain.trainingrecord.service;

import ChickenMayoDeopbab.bada.domain.callanxiety.entity.CallAnxietyState;
import ChickenMayoDeopbab.bada.domain.callanxiety.model.CallAnxietyCalculation;
import ChickenMayoDeopbab.bada.domain.callanxiety.repository.CallAnxietyStateRepository;
import ChickenMayoDeopbab.bada.domain.callanxiety.service.CallAnxietyScoreCalculator;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;
import ChickenMayoDeopbab.bada.domain.file.service.FileService;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;
import ChickenMayoDeopbab.bada.domain.session.model.TranscriptTurn;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.*;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingAnalysisMetrics;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.trainingrecord.exception.TrainingRecordStatusCode;
import ChickenMayoDeopbab.bada.domain.trainingrecord.port.FeedbackCleanupPort;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.projection.ScenarioCategoryProjection;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingRecordService {

    private final TrainingRecordRepository trainingRecordRepository;
    private final UsersRepository usersRepository;
    private final ObjectMapper objectMapper;
    private final FileService fileService;
    private final FeedbackCleanupPort feedbackCleanupPort;
    private final CallAnxietyStateRepository callAnxietyStateRepository;
    private final CallAnxietyScoreCalculator callAnxietyScoreCalculator;
    private static final String OTHER_CATEGORY = "other";
    private static final Map<String, String> CATEGORY_ICON_KEYS = Map.of(
            "work", "scenario_profile/9c59b8ee-46d0-4207-bed0-ab7136104fef",
            "daily", "scenario_profile/0c11a382-99b6-457d-80c1-4c00915c5e6c",
            "school", "scenario_profile/78b33292-2156-4665-86b7-80e0ca3535d5",
            OTHER_CATEGORY, "scenario_profile/29bdac11-0f65-4689-8ad8-f64d06f3d7b6"
    );
    private static final Set<SessionType> SCORE_SUPPORTED_TYPES = Set.of(SessionType.SCENARIO, SessionType.CUSTOM);
    private static final Set<EndReason> SCORE_ALLOWED_END_REASONS =
            Set.of(
                    EndReason.SCENARIO_DONE,
                    EndReason.END_CALL,
                    EndReason.TIMEOUT
            );

    private static final Set<EndReason> ANXIETY_SCORE_EXCLUDED_END_REASONS =
            Set.of(EndReason.ERROR, EndReason.NO_AUDIO);

    public Page<TrainingRecordResponse> getTrainingRecords(Pageable pageable) {
        Users user = getUserInfo();
        Page<TrainingRecord> records =
                trainingRecordRepository.findByUserOrderByStartedAtDesc(user, pageable);
        Map<Long, String> iconUrlsByScenarioId = resolveCategoryIconUrls(records.getContent());

        return records.map(record -> TrainingRecordResponse.from(
                record,
                iconUrlsByScenarioId.get(record.getScenarioId())
        ));
    }

    private Map<Long, String> resolveCategoryIconUrls(List<TrainingRecord> records) {
        Set<Long> scenarioIds = new HashSet<>();
        for (TrainingRecord record : records) {
            if (record.getScenarioId() != null) {
                scenarioIds.add(record.getScenarioId());
            }
        }
        if (scenarioIds.isEmpty()) {
            return Map.of();
        }

        List<ScenarioCategoryProjection> categories;
        try {
            categories = trainingRecordRepository.findScenarioCategoriesByIds(scenarioIds);
        } catch (Exception e) {
            log.warn("훈련 기록 카테고리 조회 실패, 아이콘 없이 응답 scenarioIds={}", scenarioIds, e);
            return Map.of();
        }

        Map<String, String> signedUrlsByKey = new HashMap<>();
        Map<Long, String> iconUrlsByScenarioId = new HashMap<>();
        for (ScenarioCategoryProjection category : categories) {
            String categoryName = category.getCategory();
            String key = categoryName == null
                    ? CATEGORY_ICON_KEYS.get(OTHER_CATEGORY)
                    : CATEGORY_ICON_KEYS.getOrDefault(
                            categoryName,
                            CATEGORY_ICON_KEYS.get(OTHER_CATEGORY)
                    );
            String url = resolveCategoryIconUrl(key, signedUrlsByKey);
            if (url != null) {
                iconUrlsByScenarioId.put(category.getScenarioId(), url);
            }
        }
        return iconUrlsByScenarioId;
    }

    private String resolveCategoryIconUrl(String key, Map<String, String> signedUrlsByKey) {
        if (signedUrlsByKey.containsKey(key)) {
            return signedUrlsByKey.get(key);
        }

        String url = null;
        try {
            url = fileService.generatePresignedUrl(key);
        } catch (Exception e) {
            log.warn("카테고리 아이콘 URL 서명 실패, 아이콘 없이 응답 s3Key={}", key, e);
        }
        signedUrlsByKey.put(key, url);
        return url;
    }

    public TrainingRecordDetailResponse getTrainingRecord(Long recordId) {
        Users user = getUserInfo();
        TrainingRecord record = trainingRecordRepository.findByRecordIdAndUser(recordId, user)
                .orElseThrow(() -> new ApplicationException(TrainingRecordStatusCode.RECORD_NOT_FOUND));

        String recordingUrl = resolveRecordingUrl(record.getRecordingKey());
        return TrainingRecordDetailResponse.of(
                record,
                recordingUrl,
                parseTranscript(record.getTranscript()),
                parseGoodSegments(record.getGoodSegments()).stream()
                        .map(segment -> PositiveFeedbackResponse.from(segment, recordingUrl))
                        .toList()
        );
    }

    public FeedbackResponse getFeedback(Long scenarioId) {
        Users user = getUserInfo();
        TrainingRecord trainingRecord = trainingRecordRepository.findFirstByScenarioIdAndUserOrderByEndedAtDesc(scenarioId, user)
                .orElseThrow(() -> new ApplicationException(TrainingRecordStatusCode.RECORD_NOT_FOUND));

        return FeedbackResponse.of(
                trainingRecord,
                parseGoodSegments(trainingRecord.getGoodSegments()),
                resolveRecordingUrl(trainingRecord.getRecordingKey()),
                parseTranscript(trainingRecord.getTranscript())
        );
    }

    @Transactional
    public void deleteTrainingRecord(Long recordId) {
        Users user = getUserInfo();
        TrainingRecord record = trainingRecordRepository.findByRecordIdAndUser(recordId, user)
                .orElseThrow(() -> new ApplicationException(TrainingRecordStatusCode.RECORD_NOT_FOUND));

        if (record.isScoreApplied()) {
            throw new ApplicationException(
                    TrainingRecordStatusCode
                            .SCORE_APPLIED_RECORD_CANNOT_BE_DELETED
            );
        }

        deleteRecordingQuietly(record.getRecordingKey());
        deleteFeedbackQuietly(record.getSessionId());

        trainingRecordRepository.delete(record);
    }

    // 회원 탈퇴 시 해당 사용자의 훈련 기록과 외부 리소스(S3 녹음, AI 피드백)를 함께 정리
    @Transactional
    public void deleteAllByUser(Users user) {
        List<TrainingRecord> records = trainingRecordRepository.findAllByUser(user);
        if (records.isEmpty()) {
            return;
        }

        for (TrainingRecord record : records) {
            deleteRecordingQuietly(record.getRecordingKey());
            deleteFeedbackQuietly(record.getSessionId());
        }

        trainingRecordRepository.deleteAll(records);
    }

    @Transactional
    public AnxietyScoreResponse recordAnxietyScore(
            String sessionId,
            Short anxietyScore
    ) {
        validateSubjectiveAnxiety(anxietyScore);

        Users user = getUserInfo();

        TrainingRecord record = trainingRecordRepository
                .findBySessionIdAndUser(sessionId, user)
                .orElseThrow(() -> new ApplicationException(
                        TrainingRecordStatusCode.RECORD_NOT_FOUND
                ));

        validateScoreApplicableTraining(record);

        if (record.getAnxietyScore() != null) {
            if (!record.hasSameAnxietyScore(anxietyScore)) {
                throw new ApplicationException(
                        TrainingRecordStatusCode
                                .ANXIETY_SCORE_ALREADY_RECORDED
                );
            }

            if (record.isScoreProcessed()) {
                return AnxietyScoreResponse.from(record);
            }
        }

        TrainingAnalysisMetrics analysis =
                record.getAnalysis();

        if (analysis == null) {
            return excludeScore(
                    record,
                    anxietyScore,
                    "MISSING_ANALYSIS"
            );
        }

        if (!analysis.isPassed()) {
            String reason =
                    analysis.getAnalysisExclusionReason();

            if (reason == null || reason.isBlank()) {
                reason = "LOW_ANALYSIS_QUALITY";
            }

            return excludeScore(
                    record,
                    anxietyScore,
                    reason
            );
        }

        if (!analysis.hasObjectiveScores()) {
            return excludeScore(
                    record,
                    anxietyScore,
                    "MISSING_OBJECTIVE_SCORE"
            );
        }

        if (!analysis.hasValidObjectiveScores()) {
            return excludeScore(
                    record,
                    anxietyScore,
                    "INVALID_OBJECTIVE_SCORE"
            );
        }

        if (!analysis.hasVersions()) {
            return excludeScore(
                    record,
                    anxietyScore,
                    "MISSING_ANALYSIS_VERSION"
            );
        }

        CallAnxietyState state = callAnxietyStateRepository
                .findByUserForUpdate(user)
                .orElseThrow(() -> new ApplicationException(
                        TrainingRecordStatusCode
                                .CALL_ANXIETY_STATE_NOT_FOUND
                ));

        if (!Objects.equals(
                state.getScoringVersion(),
                CallAnxietyState.SCORING_VERSION
        )) {
            return excludeScore(
                    record,
                    anxietyScore,
                    "SCORING_VERSION_MISMATCH"
            );
        }

        BigDecimal scoreBefore =
                state.getCurrentCallAnxietyIndex();

        CallAnxietyCalculation calculation =
                callAnxietyScoreCalculator.calculate(
                        scoreBefore,
                        analysis.getStabilityScore(),
                        analysis.getConversationScore(),
                        analysis.getFluencyScore(),
                        anxietyScore.intValue()
                );

        CallPhobiaLevel calculatedLevel =
                callAnxietyScoreCalculator.calculateLevel(
                        calculation
                                .newCurrentCallAnxietyIndex()
                );

        long scoreSequence =
                (long) state.getValidTrainingCount() + 1L;

        LocalDateTime appliedAt =
                LocalDateTime.now();


        state.applyValidTraining(
                calculation.newCurrentCallAnxietyIndex(),
                calculatedLevel
        );

        record.applyScore(
                anxietyScore,
                calculation,
                scoreBefore,
                scoreSequence,
                state.getScoringVersion(),
                appliedAt
        );

        return AnxietyScoreResponse.from(record);
    }

    private void deleteRecordingQuietly(String recordingKey) {
        if (recordingKey == null || recordingKey.isBlank()) {
            return;
        }
        try {
            fileService.deleteByKey(recordingKey);
        } catch (Exception e) {
            log.warn("녹음 파일 삭제 실패, 기록 삭제는 계속 진행 recordingKey={}", recordingKey, e);
        }
    }

    private void deleteFeedbackQuietly(String sessionId) {
        try {
            feedbackCleanupPort.deleteBySessionId(sessionId);
        } catch (Exception e) {
            log.warn("AI 피드백 정리 실패, 기록 삭제는 계속 진행 sessionId={}", sessionId, e);
        }
    }

    private String resolveRecordingUrl(String recordingKey) {
        if (recordingKey == null || recordingKey.isBlank()) {
            return null;
        }
        return fileService.generatePresignedUrl(recordingKey);
    }

    private Users getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return usersRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));
    }

    private List<TranscriptTurn> parseTranscript(String transcript) {
        return readList(transcript, new TypeReference<>() {});
    }

    private List<GoodSegment> parseGoodSegments(String goodSegments) {
        return readList(goodSegments, new TypeReference<>() {});
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> typeReference) {
        if (json == null || json.isBlank() || "null".equals(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            throw new IllegalStateException("훈련 기록 JSON 파싱에 실패했습니다.", e);
        }
    }

    private void validateSubjectiveAnxiety(
            Short anxietyScore
    ) {
        if (
                anxietyScore == null
                        || anxietyScore < 0
                        || anxietyScore > 10
        ) {
            throw new IllegalArgumentException(
                    "anxietyScore는 0~10 범위여야 합니다."
            );
        }
    }

    private void validateScoreApplicableTraining(
            TrainingRecord record
    ) {
        if (!SCORE_SUPPORTED_TYPES.contains(
                record.getSessionType()
        )) {
            throw new ApplicationException(
                    TrainingRecordStatusCode
                            .ANXIETY_SCORE_NOT_ALLOWED
            );
        }

        if (!SCORE_ALLOWED_END_REASONS.contains(
                record.getEndReason()
        )) {
            throw new ApplicationException(
                    TrainingRecordStatusCode
                            .ANXIETY_SCORE_NOT_ALLOWED
            );
        }
    }

    private AnxietyScoreResponse excludeScore(
            TrainingRecord record,
            Short anxietyScore,
            String exclusionReason
    ) {
        record.excludeScore(
                anxietyScore,
                exclusionReason,
                CallAnxietyState.SCORING_VERSION
        );
        return AnxietyScoreResponse.from(record);
    }
}
