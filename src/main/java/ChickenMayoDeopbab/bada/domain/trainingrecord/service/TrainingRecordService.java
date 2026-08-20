package ChickenMayoDeopbab.bada.domain.trainingrecord.service;

import ChickenMayoDeopbab.bada.domain.file.service.FileService;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;
import ChickenMayoDeopbab.bada.domain.session.model.TranscriptTurn;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.*;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.trainingrecord.exception.TrainingRecordStatusCode;
import ChickenMayoDeopbab.bada.domain.trainingrecord.port.FeedbackCleanupPort;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
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

import java.util.List;
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

    private static final Set<EndReason> ANXIETY_SCORE_EXCLUDED_END_REASONS =
            Set.of(EndReason.ERROR, EndReason.NO_AUDIO);

    public Page<TrainingRecordResponse> getTrainingRecords(Pageable pageable) {
        Users user = getUserInfo();
        return trainingRecordRepository.findByUserOrderByStartedAtDesc(user, pageable)
                .map(TrainingRecordResponse::from);
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
                resolveRecordingUrl(trainingRecord.getRecordingKey())
        );
    }

    @Transactional
    public void deleteTrainingRecord(Long recordId) {
        Users user = getUserInfo();
        TrainingRecord record = trainingRecordRepository.findByRecordIdAndUser(recordId, user)
                .orElseThrow(() -> new ApplicationException(TrainingRecordStatusCode.RECORD_NOT_FOUND));

        deleteRecordingQuietly(record.getRecordingKey());
        deleteFeedbackQuietly(record.getSessionId());

        trainingRecordRepository.delete(record);
    }

    @Transactional
    public AnxietyScoreResponse recordAnxietyScore(String sessionId, Short score) {
        Users user = getUserInfo();
        TrainingRecord record = trainingRecordRepository.findBySessionIdAndUser(sessionId, user)
                .orElseThrow(() -> new ApplicationException(TrainingRecordStatusCode.RECORD_NOT_FOUND));
        Boolean isScenarioTraining =
                record.getSessionType() == SessionType.SCENARIO;

        boolean isExcludedEndReason =
                ANXIETY_SCORE_EXCLUDED_END_REASONS.contains(
                        record.getEndReason()
                );

        if (!isScenarioTraining || isExcludedEndReason) {
            throw new ApplicationException(
                    TrainingRecordStatusCode.ANXIETY_SCORE_NOT_ALLOWED
            );
        }

        if (record.getAnxietyScore() != null) {
            throw new ApplicationException(
                    TrainingRecordStatusCode.ANXIETY_SCORE_ALREADY_RECORDED
            );
        }

        record.recordAnxietyScore(score);

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
}
