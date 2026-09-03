package ChickenMayoDeopbab.bada.domain.trainingrecord.service;

import ChickenMayoDeopbab.bada.domain.callanxiety.entity.CallAnxietyState;
import ChickenMayoDeopbab.bada.domain.callanxiety.repository.CallAnxietyStateRepository;
import ChickenMayoDeopbab.bada.domain.callanxiety.service.CallAnxietyScoreCalculator;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;
import ChickenMayoDeopbab.bada.domain.file.service.FileService;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request.TrainingAnalysisRequest;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.AnxietyScoreResponse;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.AnalysisQualityStatus;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingAnalysisMetrics;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.trainingrecord.exception.TrainingRecordStatusCode;
import ChickenMayoDeopbab.bada.domain.trainingrecord.port.FeedbackCleanupPort;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrainingRecordScoreApplicationTest {

    private final TrainingRecordRepository trainingRecordRepository =
            mock(TrainingRecordRepository.class);

    private final UsersRepository usersRepository =
            mock(UsersRepository.class);

    private final FileService fileService =
            mock(FileService.class);

    private final FeedbackCleanupPort feedbackCleanupPort =
            mock(FeedbackCleanupPort.class);

    private final CallAnxietyStateRepository callAnxietyStateRepository =
            mock(CallAnxietyStateRepository.class);

    private final TrainingRecordService service =
            new TrainingRecordService(
                    trainingRecordRepository,
                    usersRepository,
                    new ObjectMapper(),
                    fileService,
                    feedbackCleanupPort,
                    callAnxietyStateRepository,
                    new CallAnxietyScoreCalculator()
            );

    private final Users user = mock(Users.class);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "tester",
                        null,
                        List.of()
                )
        );

        when(usersRepository.findByUsername("tester"))
                .thenReturn(Optional.of(user));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void appliesSubjectiveAndObjectiveScores() {
        TrainingRecord record = createRecord(passedAnalysis());
        CallAnxietyState state = createState();

        mockRecordAndState(record, state);

        AnxietyScoreResponse response = service.recordAnxietyScore(
                "session-1",
                (short) 5
        );

        assertThat(response.scoreApplied()).isTrue();
        assertThat(response.scoreExclusionReason()).isNull();
        assertThat(response.performanceScore())
                .isEqualByComparingTo("75.0000");
        assertThat(response.performanceRiskScore())
                .isEqualByComparingTo("2.0000");
        assertThat(response.subjectiveAnxietyScore())
                .isEqualByComparingTo("3.0000");
        assertThat(response.trainingStateIndex())
                .isEqualByComparingTo("2.6000");
        assertThat(response.scoreBefore())
                .isEqualByComparingTo("3.0000");
        assertThat(response.scoreAfter())
                .isEqualByComparingTo("2.9600");
        assertThat(response.scoreSequence()).isEqualTo(1L);
        assertThat(response.scoringVersion())
                .isEqualTo(CallAnxietyState.SCORING_VERSION);

        assertThat(state.getCurrentCallAnxietyIndex())
                .isEqualByComparingTo("2.9600");
        assertThat(state.getValidTrainingCount()).isEqualTo(1);
    }

    @Test
    void returnsExistingResultForSameRequest() {
        TrainingRecord record = createRecord(passedAnalysis());
        CallAnxietyState state = createState();

        mockRecordAndState(record, state);

        service.recordAnxietyScore("session-1", (short) 5);

        AnxietyScoreResponse retry = service.recordAnxietyScore(
                "session-1",
                (short) 5
        );

        assertThat(retry.scoreApplied()).isTrue();
        assertThat(retry.scoreSequence()).isEqualTo(1L);
        assertThat(state.getValidTrainingCount()).isEqualTo(1);

        verify(callAnxietyStateRepository, times(1))
                .findByUserForUpdate(user);
    }

    @Test
    void rejectsDifferentScoreForProcessedSession() {
        TrainingRecord record = createRecord(passedAnalysis());
        CallAnxietyState state = createState();

        mockRecordAndState(record, state);

        service.recordAnxietyScore("session-1", (short) 5);

        assertThatThrownBy(() ->
                service.recordAnxietyScore("session-1", (short) 6)
        )
                .isInstanceOf(ApplicationException.class)
                .extracting(exception ->
                        ((ApplicationException) exception).getStatusCode()
                )
                .isEqualTo(
                        TrainingRecordStatusCode
                                .ANXIETY_SCORE_ALREADY_RECORDED
                );

        assertThat(state.getValidTrainingCount()).isEqualTo(1);
    }

    @Test
    void storesSubjectiveScoreButExcludesFailedAnalysis() {
        TrainingRecord record = createRecord(failedAnalysis());

        when(trainingRecordRepository.findBySessionIdAndUser(
                "session-1",
                user
        )).thenReturn(Optional.of(record));

        AnxietyScoreResponse response = service.recordAnxietyScore(
                "session-1",
                (short) 7
        );

        assertThat(response.anxietyScore()).isEqualTo((short) 7);
        assertThat(response.scoreApplied()).isFalse();
        assertThat(response.scoreExclusionReason())
                .isEqualTo("INSUFFICIENT_USER_SPEECH");
        assertThat(response.scoreAfter()).isNull();

        verify(callAnxietyStateRepository, never())
                .findByUserForUpdate(any());
    }

    @Test
    void storesSubjectiveScoreWhenInitialStateIsMissing() {
        TrainingRecord record = createRecord(passedAnalysis());

        when(trainingRecordRepository.findBySessionIdAndUser(
                "session-1",
                user
        )).thenReturn(Optional.of(record));

        when(callAnxietyStateRepository.findByUserForUpdate(user))
                .thenReturn(Optional.empty());

        AnxietyScoreResponse response = service.recordAnxietyScore(
                "session-1",
                (short) 5
        );

        assertThat(response.anxietyScore()).isEqualTo((short) 5);
        assertThat(response.scoreApplied()).isFalse();
        assertThat(response.scoreExclusionReason())
                .isEqualTo("MISSING_CALL_ANXIETY_STATE");
        assertThat(record.getAnxietyScore()).isEqualTo((short) 5);
        assertThat(record.getScoreApplied()).isFalse();
        assertThat(record.getScoreExclusionReason())
                .isEqualTo("MISSING_CALL_ANXIETY_STATE");
    }

    private void mockRecordAndState(
            TrainingRecord record,
            CallAnxietyState state
    ) {
        when(trainingRecordRepository.findBySessionIdAndUser(
                "session-1",
                user
        )).thenReturn(Optional.of(record));

        when(callAnxietyStateRepository.findByUserForUpdate(user))
                .thenReturn(Optional.of(state));
    }

    private TrainingRecord createRecord(
            TrainingAnalysisMetrics analysis
    ) {
        LocalDateTime now = LocalDateTime.now();

        return TrainingRecord.builder()
                .user(user)
                .sessionId("session-1")
                .scenarioId(1L)
                .scenarioName("음식점 예약")
                .scenarioVersion("SCENARIO_V1")
                .difficulty("MEDIUM")
                .sessionType(SessionType.SCENARIO)
                .endReason(EndReason.SCENARIO_DONE)
                .startedAt(now.minusMinutes(1))
                .endedAt(now)
                .durationSeconds(60L)
                .analysis(analysis)
                .build();
    }

    private CallAnxietyState createState() {
        return CallAnxietyState.create(
                user,
                new BigDecimal("3.0"),
                CallPhobiaLevel.LEVEL_3,
                LocalDateTime.now()
        );
    }

    private TrainingAnalysisMetrics passedAnalysis() {
        return TrainingAnalysisMetrics.from(
                new TrainingAnalysisRequest(
                        new BigDecimal("75"),
                        new BigDecimal("75"),
                        new BigDecimal("75"),
                        6000L,
                        1000L,
                        1200L,
                        2,
                        1000L,
                        4000L,
                        2,
                        4,
                        AnalysisQualityStatus.PASS,
                        null,
                        "SPEECH_ANALYZER_V1",
                        "ANALYSIS_POLICY_V1"
                )
        );
    }

    private TrainingAnalysisMetrics failedAnalysis() {
        return TrainingAnalysisMetrics.from(
                new TrainingAnalysisRequest(
                        null,
                        null,
                        null,
                        1000L,
                        1000L,
                        1200L,
                        1,
                        0L,
                        0L,
                        0,
                        4,
                        AnalysisQualityStatus.FAIL,
                        "INSUFFICIENT_USER_SPEECH",
                        "SPEECH_ANALYZER_V1",
                        "ANALYSIS_POLICY_V1"
                )
        );
    }
}
