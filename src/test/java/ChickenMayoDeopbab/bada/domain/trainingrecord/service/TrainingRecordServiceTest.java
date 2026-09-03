package ChickenMayoDeopbab.bada.domain.trainingrecord.service;

import ChickenMayoDeopbab.bada.domain.callanxiety.repository.CallAnxietyStateRepository;
import ChickenMayoDeopbab.bada.domain.callanxiety.service.CallAnxietyScoreCalculator;
import ChickenMayoDeopbab.bada.domain.file.service.FileService;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;
import ChickenMayoDeopbab.bada.domain.session.model.TranscriptTurn;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.FeedbackResponse;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.TrainingRecordResponse;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.trainingrecord.exception.TrainingRecordStatusCode;
import ChickenMayoDeopbab.bada.domain.trainingrecord.port.FeedbackCleanupPort;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.projection.ScenarioCategoryProjection;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    private void loginForList() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("junha", null));
        when(usersRepository.findByUsername("junha")).thenReturn(Optional.of(user));
    }

    private TrainingRecord listRecord(Long scenarioId, String scenarioName) {
        return TrainingRecord.builder()
                .user(user)
                .sessionId("sess-" + scenarioId)
                .scenarioId(scenarioId)
                .scenarioName(scenarioName)
                .sessionType(SessionType.SCENARIO)
                .startedAt(LocalDateTime.of(2026, 9, 1, 10, 0))
                .durationSeconds(60L)
                .build();
    }

    private ScenarioCategoryProjection scenarioMedia(
            Long scenarioId,
            String category,
            String scenarioImage
    ) {
        ScenarioCategoryProjection projection = mock(ScenarioCategoryProjection.class);
        when(projection.getScenarioId()).thenReturn(scenarioId);
        when(projection.getCategory()).thenReturn(category);
        when(projection.getScenarioImage()).thenReturn(scenarioImage);
        return projection;
    }

    @Test
    void trainingRecordsReturnScenarioImagesAndCategoryIconsSeparately() {
        loginForList();
        PageRequest pageable = PageRequest.of(0, 20);
        List<TrainingRecord> records = List.of(
                listRecord(10L, "업무 문의하기"),
                listRecord(11L, "병원 예약하기"),
                listRecord(12L, "배달 문의하기"),
                listRecord(13L, "학교 문의하기"),
                listRecord(14L, "기타 문의하기")
        );
        when(trainingRecordRepository.findByUserOrderByStartedAtDesc(user, pageable))
                .thenReturn(new PageImpl<>(records, pageable, records.size()));
        ScenarioCategoryProjection workCategory =
                scenarioMedia(10L, "work", "scenario-images/work.png");
        ScenarioCategoryProjection firstDailyCategory =
                scenarioMedia(11L, "daily", "scenario-images/hospital.png");
        ScenarioCategoryProjection secondDailyCategory =
                scenarioMedia(12L, "daily", "scenario-images/delivery.png");
        ScenarioCategoryProjection schoolCategory =
                scenarioMedia(13L, "school", "scenario-images/school.png");
        ScenarioCategoryProjection otherCategory =
                scenarioMedia(14L, "other", "scenario-images/other.png");
        when(trainingRecordRepository.findScenarioCategoriesByIds(anyCollection()))
                .thenReturn(List.of(
                        workCategory,
                        firstDailyCategory,
                        secondDailyCategory,
                        schoolCategory,
                        otherCategory
                ));
        when(fileService.generatePresignedUrl(
                "scenario_profile/9c59b8ee-46d0-4207-bed0-ab7136104fef"
        )).thenReturn("https://s3/work.svg");
        when(fileService.generatePresignedUrl(
                "scenario_profile/0c11a382-99b6-457d-80c1-4c00915c5e6c"
        )).thenReturn("https://s3/daily.svg");
        when(fileService.generatePresignedUrl(
                "scenario_profile/78b33292-2156-4665-86b7-80e0ca3535d5"
        )).thenReturn("https://s3/school.svg");
        when(fileService.generatePresignedUrl(
                "scenario_profile/29bdac11-0f65-4689-8ad8-f64d06f3d7b6"
        )).thenReturn("https://s3/other.svg");
        when(fileService.generatePresignedUrl("scenario-images/work.png"))
                .thenReturn("https://s3/work.png");
        when(fileService.generatePresignedUrl("scenario-images/hospital.png"))
                .thenReturn("https://s3/hospital.png");
        when(fileService.generatePresignedUrl("scenario-images/delivery.png"))
                .thenReturn("https://s3/delivery.png");
        when(fileService.generatePresignedUrl("scenario-images/school.png"))
                .thenReturn("https://s3/school.png");
        when(fileService.generatePresignedUrl("scenario-images/other.png"))
                .thenReturn("https://s3/other.png");

        Page<TrainingRecordResponse> response = service.getTrainingRecords(pageable);

        assertThat(response.getContent())
                .extracting(TrainingRecordResponse::scenarioImage)
                .containsExactly(
                        "https://s3/work.png",
                        "https://s3/hospital.png",
                        "https://s3/delivery.png",
                        "https://s3/school.png",
                        "https://s3/other.png"
                );
        assertThat(response.getContent())
                .extracting(TrainingRecordResponse::categoryIconUrl)
                .containsExactly(
                        "https://s3/work.svg",
                        "https://s3/daily.svg",
                        "https://s3/daily.svg",
                        "https://s3/school.svg",
                        "https://s3/other.svg"
                );
        verify(fileService, times(9)).generatePresignedUrl(anyString());
        verify(fileService, times(1)).generatePresignedUrl(
                "scenario_profile/0c11a382-99b6-457d-80c1-4c00915c5e6c"
        );
    }

    @Test
    void unknownCategoryUsesOtherIcon() {
        loginForList();
        PageRequest pageable = PageRequest.of(0, 20);
        TrainingRecord record = listRecord(10L, "기타 문의하기");
        when(trainingRecordRepository.findByUserOrderByStartedAtDesc(user, pageable))
                .thenReturn(new PageImpl<>(List.of(record), pageable, 1));
        ScenarioCategoryProjection unknownCategory = scenarioMedia(10L, "legacy-category", null);
        when(trainingRecordRepository.findScenarioCategoriesByIds(anyCollection()))
                .thenReturn(List.of(unknownCategory));
        when(fileService.generatePresignedUrl(
                "scenario_profile/29bdac11-0f65-4689-8ad8-f64d06f3d7b6"
        )).thenReturn("https://s3/other.svg");

        TrainingRecordResponse response = service.getTrainingRecords(pageable).getContent().getFirst();

        assertThat(response.scenarioImage()).isNull();
        assertThat(response.categoryIconUrl()).isEqualTo("https://s3/other.svg");
    }

    @Test
    void categoryLookupFailureKeepsTrainingRecordsAvailable() {
        loginForList();
        PageRequest pageable = PageRequest.of(0, 20);
        TrainingRecord record = listRecord(10L, "병원 예약하기");
        when(trainingRecordRepository.findByUserOrderByStartedAtDesc(user, pageable))
                .thenReturn(new PageImpl<>(List.of(record), pageable, 1));
        when(trainingRecordRepository.findScenarioCategoriesByIds(anyCollection()))
                .thenThrow(new RuntimeException("query failed"));

        TrainingRecordResponse response = service.getTrainingRecords(pageable).getContent().getFirst();

        assertThat(response.scenarioImage()).isNull();
        assertThat(response.categoryIconUrl()).isNull();
        verifyNoInteractions(fileService);
    }

    @Test
    void scenarioImageSigningFailureDoesNotHideCategoryIcon() {
        loginForList();
        PageRequest pageable = PageRequest.of(0, 20);
        TrainingRecord record = listRecord(10L, "병원 예약하기");
        when(trainingRecordRepository.findByUserOrderByStartedAtDesc(user, pageable))
                .thenReturn(new PageImpl<>(List.of(record), pageable, 1));
        ScenarioCategoryProjection media =
                scenarioMedia(10L, "daily", "scenario-images/hospital.png");
        when(trainingRecordRepository.findScenarioCategoriesByIds(anyCollection()))
                .thenReturn(List.of(media));
        when(fileService.generatePresignedUrl("scenario-images/hospital.png"))
                .thenThrow(new RuntimeException("signature failed"));
        when(fileService.generatePresignedUrl(
                "scenario_profile/0c11a382-99b6-457d-80c1-4c00915c5e6c"
        )).thenReturn("https://s3/daily.svg");

        TrainingRecordResponse response = service.getTrainingRecords(pageable).getContent().getFirst();

        assertThat(response.scenarioImage()).isNull();
        assertThat(response.categoryIconUrl()).isEqualTo("https://s3/daily.svg");
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
