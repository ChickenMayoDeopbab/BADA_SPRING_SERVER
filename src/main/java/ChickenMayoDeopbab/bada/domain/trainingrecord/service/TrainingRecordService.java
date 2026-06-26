package ChickenMayoDeopbab.bada.domain.trainingrecord.service;

import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;
import ChickenMayoDeopbab.bada.domain.session.model.TranscriptTurn;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.FeedbackResponse;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.PositiveFeedbackResponse;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.TrainingRecordDetailResponse;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.TrainingRecordResponse;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.trainingrecord.exception.TrainingRecordStatusCode;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingRecordService {

    private final TrainingRecordRepository trainingRecordRepository;
    private final UsersRepository usersRepository;
    private final ObjectMapper objectMapper;

    public Page<TrainingRecordResponse> getTrainingRecords(Pageable pageable) {
        Users user = getUserInfo();
        return trainingRecordRepository.findByUserOrderByStartedAtDesc(user, pageable)
                .map(TrainingRecordResponse::from);
    }

    public TrainingRecordDetailResponse getTrainingRecord(Long recordId) {
        Users user = getUserInfo();
        TrainingRecord record = trainingRecordRepository.findByRecordIdAndUser(recordId, user)
                .orElseThrow(() -> new ApplicationException(TrainingRecordStatusCode.RECORD_NOT_FOUND));

        return TrainingRecordDetailResponse.of(
                record,
                parseTranscript(record.getTranscript()),
                parseGoodSegments(record.getGoodSegments()).stream()
                        .map(PositiveFeedbackResponse::from)
                        .toList()
        );
    }

    public FeedbackResponse getFeedback(Long scenarioId) {
        Users user = getUserInfo();
        TrainingRecord trainingRecord = trainingRecordRepository.findFirstByScenarioIdAndUserOrderByEndedAtDesc(scenarioId, user)
                .orElseThrow(() -> new ApplicationException(TrainingRecordStatusCode.RECORD_NOT_FOUND));

        return FeedbackResponse.of(
                trainingRecord,
                parseGoodSegments(trainingRecord.getGoodSegments())
        );
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
