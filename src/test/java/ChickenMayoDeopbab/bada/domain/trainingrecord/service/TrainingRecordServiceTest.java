package ChickenMayoDeopbab.bada.domain.trainingrecord.service;

import ChickenMayoDeopbab.bada.domain.callanxiety.repository.CallAnxietyStateRepository;
import ChickenMayoDeopbab.bada.domain.callanxiety.service.CallAnxietyScoreCalculator;
import ChickenMayoDeopbab.bada.domain.file.service.FileService;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;
import ChickenMayoDeopbab.bada.domain.session.model.TranscriptTurn;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.FeedbackResponse;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.trainingrecord.exception.TrainingRecordStatusCode;
import ChickenMayoDeopbab.bada.domain.trainingrecord.port.FeedbackCleanupPort;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TrainingRecordServiceTest {

    private final TrainingRecordRepository trainingRecordRepository = mock(TrainingRecordRepository.class);
    private final UsersRepository usersRepository = mock(UsersRepository.class);
    private final FileService fileService = mock(FileService.class);
    private final FeedbackCleanupPort feedbackCleanupPort = mock(FeedbackCleanupPort.class);
    private final CallAnxietyStateRepository callAnxietyStateRepository =
            mock(CallAnxietyStateRepository.class);
    private final TrainingRecordService service = new TrainingRecordService(
            trainingRecordRepository,
            usersRepository,
            new ObjectMapper(),
            fileService,
            feedbackCleanupPort,
            callAnxietyStateRepository,
            new CallAnxietyScoreCalculator()
    );

    private final Users user = mock(Users.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private TrainingRecord record(String recordingKey) {
        return record("sess-1", recordingKey);
    }

    private TrainingRecord record(String sessionId, String recordingKey) {
        return TrainingRecord.builder()
                .user(user)
                .sessionId(sessionId)
                .recordingKey(recordingKey)
                .build();
    }

    private void login(TrainingRecord found) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("junha", null));
        when(usersRepository.findByUsername("junha")).thenReturn(Optional.of(user));
        when(trainingRecordRepository.findByRecordIdAndUser(1L, user))
                .thenReturn(Optional.ofNullable(found));
    }

    @Test
    void deleteRemovesRowRecordingAndFeedback() {
        TrainingRecord record = record("recordings/sess-1.wav");
        login(record);

        service.deleteTrainingRecord(1L);

        verify(fileService).deleteByKey("recordings/sess-1.wav");
        verify(feedbackCleanupPort).deleteBySessionId("sess-1");
        verify(trainingRecordRepository).delete(record);
    }

    @Test
    void deleteMissingOrOthersRecordThrowsNotFoundAndTouchesNothing() {
        login(null);

        assertThatThrownBy(() -> service.deleteTrainingRecord(1L))
                .isInstanceOf(ApplicationException.class)
                .extracting(ex -> ((ApplicationException) ex).getStatusCode())
                .isEqualTo(TrainingRecordStatusCode.RECORD_NOT_FOUND);

        verify(fileService, never()).deleteByKey(anyString());
        verify(feedbackCleanupPort, never()).deleteBySessionId(anyString());
        verify(trainingRecordRepository, never()).delete(any());
    }

    @Test
    void recordingDeleteFailureStillDeletesRow() {
        TrainingRecord record = record("recordings/sess-1.wav");
        login(record);
        doThrow(new RuntimeException("s3 down")).when(fileService).deleteByKey(anyString());

        service.deleteTrainingRecord(1L);

        verify(feedbackCleanupPort).deleteBySessionId("sess-1");
        verify(trainingRecordRepository).delete(record);
    }

    @Test
    void feedbackCleanupFailureStillDeletesRow() {
        TrainingRecord record = record("recordings/sess-1.wav");
        login(record);
        doThrow(new RuntimeException("ai down")).when(feedbackCleanupPort).deleteBySessionId(anyString());

        service.deleteTrainingRecord(1L);

        verify(fileService).deleteByKey("recordings/sess-1.wav");
        verify(trainingRecordRepository).delete(record);
    }

    @Test
    void nullRecordingKeySkipsS3Delete() {
        TrainingRecord record = record(null);
        login(record);

        service.deleteTrainingRecord(1L);

        verify(fileService, never()).deleteByKey(anyString());
        verify(feedbackCleanupPort).deleteBySessionId("sess-1");
        verify(trainingRecordRepository).delete(record);
    }

    @Test
    void deleteAllByUserRemovesEveryRecordWithItsRecordingAndFeedback() {
        TrainingRecord first = record("sess-1", "recordings/sess-1.wav");
        TrainingRecord second = record("sess-2", "recordings/sess-2.wav");
        List<TrainingRecord> records = List.of(first, second);
        when(trainingRecordRepository.findAllByUser(user)).thenReturn(records);

        service.deleteAllByUser(user);

        InOrder order = inOrder(fileService, feedbackCleanupPort, trainingRecordRepository);
        order.verify(fileService).deleteByKey("recordings/sess-1.wav");
        order.verify(feedbackCleanupPort).deleteBySessionId("sess-1");
        order.verify(fileService).deleteByKey("recordings/sess-2.wav");
        order.verify(feedbackCleanupPort).deleteBySessionId("sess-2");
        order.verify(trainingRecordRepository).deleteAll(records);
    }

    @Test
    void deleteAllByUserWithoutRecordsTouchesNothing() {
        when(trainingRecordRepository.findAllByUser(user)).thenReturn(List.of());

        service.deleteAllByUser(user);

        verify(trainingRecordRepository, never()).deleteAll(any());
        verifyNoInteractions(fileService, feedbackCleanupPort);
    }

    @Test
    void deleteAllByUserContinuesWhenExternalCleanupFails() {
        List<TrainingRecord> records = List.of(record("sess-1", "recordings/sess-1.wav"));
        when(trainingRecordRepository.findAllByUser(user)).thenReturn(records);
        doThrow(new RuntimeException("s3 down")).when(fileService).deleteByKey(anyString());
        doThrow(new RuntimeException("ai down")).when(feedbackCleanupPort).deleteBySessionId(anyString());

        service.deleteAllByUser(user);

        verify(trainingRecordRepository).deleteAll(records);
    }

    private static final String TRANSCRIPT_JSON = """
            [
              {"role":"user","text":"여보세요"},
              {"role":"assistant","text":"네, 안녕하세요"}
            ]
            """;

    private static final String GOOD_SEGMENTS_JSON = """
            [
              {"start":1.5,"end":3.0,"good_point":"인사를 또렷하게 했어요"}
            ]
            """;

    private TrainingRecord feedbackRecord(String transcript, String goodSegments, String recordingKey) {
        return TrainingRecord.builder()
                .user(user)
                .sessionId("sess-1")
                .scenarioId(10L)
                .scenarioName("병원 예약 전화")
                .sessionType(SessionType.SCENARIO)
                .endReason(EndReason.SCENARIO_DONE)
                .startedAt(LocalDateTime.of(2026, 1, 1, 10, 0, 0))
                .endedAt(LocalDateTime.of(2026, 1, 1, 10, 1, 30))
                .durationSeconds(90L)
                .transcript(transcript)
                .goodSegments(goodSegments)
                .recordingKey(recordingKey)
                .build();
    }

    private void loginForFeedback(TrainingRecord found) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("junha", null));
        when(usersRepository.findByUsername("junha")).thenReturn(Optional.of(user));
        when(trainingRecordRepository.findFirstByScenarioIdAndUserOrderByEndedAtDesc(10L, user))
                .thenReturn(Optional.ofNullable(found));
    }

    @Test
    void feedbackReturnsParsedTranscriptWithOtherFields() {
        loginForFeedback(feedbackRecord(TRANSCRIPT_JSON, GOOD_SEGMENTS_JSON, "recordings/sess-1.wav"));
        when(fileService.generatePresignedUrl("recordings/sess-1.wav")).thenReturn("https://s3/sess-1.wav");

        FeedbackResponse response = service.getFeedback(10L);

        assertThat(response.sessionType()).isEqualTo(SessionType.SCENARIO);
        assertThat(response.scenarioName()).isEqualTo("병원 예약 전화");
        assertThat(response.trainingTime()).isEqualTo(LocalTime.of(0, 1, 30));
        assertThat(response.recordingUrl()).isEqualTo("https://s3/sess-1.wav");
        assertThat(response.goodSegments())
                .containsExactly(new GoodSegment(1.5, 3.0, "인사를 또렷하게 했어요"));
        assertThat(response.transcript()).containsExactly(
                new TranscriptTurn("user", "여보세요"),
                new TranscriptTurn("assistant", "네, 안녕하세요")
        );
    }

    @Test
    void feedbackReturnsEmptyTranscriptWhenTranscriptIsNull() {
        loginForFeedback(feedbackRecord(null, GOOD_SEGMENTS_JSON, null));

        FeedbackResponse response = service.getFeedback(10L);

        assertThat(response.transcript()).isEmpty();
        assertThat(response.goodSegments()).hasSize(1);
    }

    @Test
    void feedbackReturnsEmptyTranscriptWhenTranscriptIsBlankOrNullLiteral() {
        loginForFeedback(feedbackRecord("   ", null, null));
        assertThat(service.getFeedback(10L).transcript()).isEmpty();

        loginForFeedback(feedbackRecord("null", null, null));
        assertThat(service.getFeedback(10L).transcript()).isEmpty();
    }

    @Test
    void feedbackWithoutRecordingKeyReturnsNullUrlAndSkipsPresign() {
        loginForFeedback(feedbackRecord(TRANSCRIPT_JSON, GOOD_SEGMENTS_JSON, null));

        FeedbackResponse response = service.getFeedback(10L);

        assertThat(response.recordingUrl()).isNull();
        assertThat(response.transcript()).hasSize(2);
        verify(fileService, never()).generatePresignedUrl(anyString());
    }

    @Test
    void feedbackWithBrokenTranscriptJsonThrowsIllegalState() {
        loginForFeedback(feedbackRecord("{not-json", GOOD_SEGMENTS_JSON, null));

        assertThatThrownBy(() -> service.getFeedback(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("훈련 기록 JSON 파싱에 실패했습니다.");
    }

    @Test
    void feedbackWithoutRecordThrowsNotFound() {
        loginForFeedback(null);

        assertThatThrownBy(() -> service.getFeedback(10L))
                .isInstanceOf(ApplicationException.class)
                .extracting(ex -> ((ApplicationException) ex).getStatusCode())
                .isEqualTo(TrainingRecordStatusCode.RECORD_NOT_FOUND);

        verifyNoInteractions(fileService);
    }
}
