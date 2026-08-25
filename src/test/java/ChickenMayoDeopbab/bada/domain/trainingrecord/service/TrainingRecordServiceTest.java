package ChickenMayoDeopbab.bada.domain.trainingrecord.service;

import ChickenMayoDeopbab.bada.domain.callanxiety.repository.CallAnxietyStateRepository;
import ChickenMayoDeopbab.bada.domain.callanxiety.service.CallAnxietyScoreCalculator;
import ChickenMayoDeopbab.bada.domain.file.service.FileService;
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

import java.util.List;
import java.util.Optional;

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
}
