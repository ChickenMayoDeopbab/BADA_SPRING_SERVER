package ChickenMayoDeopbab.bada.domain.adminstatistics.service;

import ChickenMayoDeopbab.bada.domain.adminstatistics.entity.*;
import ChickenMayoDeopbab.bada.domain.adminstatistics.model.*;
import ChickenMayoDeopbab.bada.domain.adminstatistics.repository.*;
import ChickenMayoDeopbab.bada.domain.adminstatistics.dto.request.EvaluationRequest;
import ChickenMayoDeopbab.bada.domain.callanxiety.service.CallAnxietyScoreCalculator;
import ChickenMayoDeopbab.bada.domain.callanxiety.entity.CallAnxietyState;
import ChickenMayoDeopbab.bada.domain.callanxiety.repository.CallAnxietyStateRepository;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.*;
import ChickenMayoDeopbab.bada.domain.diagnosis.repository.DiagnosisResultRepository;
import ChickenMayoDeopbab.bada.domain.session.enums.*;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingAnalysisMetrics;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.AnalysisQualityStatus;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request.TrainingAnalysisRequest;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.trainingrecord.service.TrainingRecordService;
import ChickenMayoDeopbab.bada.domain.trainingrecord.service.RecordingPlaybackService;
import ChickenMayoDeopbab.bada.domain.trainingrecord.port.TrainingDataCleanupPort;
import ChickenMayoDeopbab.bada.domain.trainingrecord.model.TrainingDataRemoved;
import ChickenMayoDeopbab.bada.domain.file.service.FileService;
import ChickenMayoDeopbab.bada.domain.legalconsent.service.SensitiveInformationConsentPolicy;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import javax.sql.DataSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest(properties = {
        "spring.datasource.url=${BADA_EVALUATION_TEST_DB_URL}",
        "spring.datasource.username=${BADA_EVALUATION_TEST_DB_USER:postgres}", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "BADA_EVALUATION_TEST_DB_URL", matches = ".+")
class EvaluationPersistenceTest {
    @Autowired private EntityManager entityManager;
    @Autowired private EvaluationRunRepository runs;
    @Autowired private EvaluationCohortRepository cohorts;
    @Autowired private UsersRepository users;
    @Autowired private TrainingRecordRepository records;
    @Autowired private DiagnosisResultRepository diagnosesRepository;
    @Autowired private CallAnxietyStateRepository states;
    @Autowired private PlatformTransactionManager transactionManager;

    @TestConfiguration
    static class TemporaryDatabaseConfiguration {
        @Bean
        DataSource dataSource(Environment environment) {
            String url = environment.getRequiredProperty("BADA_EVALUATION_TEST_DB_URL");
            if (!url.matches("jdbc:postgresql://(127\\.0\\.0\\.1|localhost):\\d+/bada_evaluation_test")) {
                throw new IllegalArgumentException("영속성 테스트는 로컬 bada_evaluation_test DB만 사용할 수 있습니다.");
            }
            DriverManagerDataSource source = new DriverManagerDataSource();
            source.setDriverClassName("org.postgresql.Driver"); source.setUrl(url);
            source.setUsername(environment.getProperty("BADA_EVALUATION_TEST_DB_USER", "postgres"));
            source.setPassword(""); return source;
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentDeletionAndScoreApplicationKeepCountAndScoresConsistent() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        long[] ids = transaction.execute(status -> {
            Users user = Users.builder().username("evaluation-concurrency").name("동시성 검증").build();
            entityManager.persist(user);
            CallAnxietyState state = CallAnxietyState.create(user, BigDecimal.valueOf(3), CallPhobiaLevel.LEVEL_3, LocalDateTime.now());
            entityManager.persist(state);
            List<TrainingRecord> saved = new ArrayList<>();
            CallAnxietyScoreCalculator calculator = new CallAnxietyScoreCalculator();
            for (int index = 1; index <= 4; index++) {
                LocalDateTime now = LocalDateTime.now();
                var input = new TrainingAnalysisRequest(BigDecimal.valueOf(80), BigDecimal.valueOf(75), BigDecimal.valueOf(70),
                        6000L, 1000L, 100L, 2, 0L, 4000L, 2, 2, AnalysisQualityStatus.PASS, null, "V3", "P3");
                TrainingRecord record = TrainingRecord.builder().user(user).sessionId("concurrent-" + index).scenarioName("예약")
                        .sessionType(SessionType.CUSTOM).endReason(EndReason.END_CALL).startedAt(now.minusMinutes(1))
                        .endedAt(now).durationSeconds(60L).analysis(TrainingAnalysisMetrics.from(input)).build();
                if (index <= 3) {
                    BigDecimal before = state.getCurrentCallAnxietyIndex();
                    var calculation = calculator.calculate(before, BigDecimal.valueOf(80), BigDecimal.valueOf(75), BigDecimal.valueOf(70), 5);
                    record.applyScore((short) 5, calculation, before, index, CallAnxietyState.SCORING_VERSION, now);
                    state.applyValidTraining(calculation.newCurrentCallAnxietyIndex(), calculator.calculateLevel(calculation.newCurrentCallAnxietyIndex()));
                }
                entityManager.persist(record); saved.add(record);
            }
            entityManager.flush(); return new long[] {user.getUserId(), saved.get(1).getRecordId()};
        });
        TrainingRecordService service = new TrainingRecordService(records, users, new ObjectMapper(), mock(FileService.class),
                mock(TrainingDataCleanupPort.class), states, new CallAnxietyScoreCalculator(), mock(SensitiveInformationConsentPolicy.class),
                mock(RecordingPlaybackService.class), mock(ApplicationEventPublisher.class));
        CountDownLatch ready = new CountDownLatch(2);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<?>> futures = new ArrayList<>();
            for (boolean delete : List.of(true, false)) {
                futures.add(executor.submit(() -> {
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("evaluation-concurrency", null));
                    try {
                        ready.countDown();
                        if (!ready.await(5, TimeUnit.SECONDS)) { throw new IllegalStateException("동시 실행 준비 실패"); }
                        transaction.executeWithoutResult(status -> {
                            if (delete) { service.deleteTrainingRecord(ids[1]); }
                            else { service.recordAnxietyScore("concurrent-4", (short) 5); }
                        });
                    } catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
                    finally { SecurityContextHolder.clearContext(); }
                }));
            }
            for (Future<?> future : futures) { future.get(15, TimeUnit.SECONDS); }
        }
        transaction.executeWithoutResult(status -> {
            Users user = users.findById(ids[0]).orElseThrow();
            CallAnxietyState state = states.findByUser(user).orElseThrow();
            assertThat(state.getValidTrainingCount()).isEqualTo(3);
            assertThat(state.getCurrentCallAnxietyIndex()).isEqualByComparingTo("2.8916");
            assertThat(records.findAllByUser(user)).hasSize(3);
            assertThat(records.findMaximumScoreSequenceByUser(user)).isEqualTo(4);
        });
    }

    @Test
    void perfectPerformanceAndAnswerSnapshotRoundTripThroughPostgres() {
        Users user = user();
        LocalDateTime now = LocalDateTime.now();
        TrainingRecord record = TrainingRecord.builder().user(user).sessionId("perfect")
                .scenarioName("예약").sessionType(SessionType.CUSTOM).endReason(EndReason.END_CALL)
                .startedAt(now.minusMinutes(1)).endedAt(now).durationSeconds(60L).build();
        var calculation = new CallAnxietyScoreCalculator().calculate(BigDecimal.valueOf(4),
                BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), 3);
        record.applyScore((short) 3, calculation, BigDecimal.valueOf(4), 1, "CALL_ANXIETY_V1", now);
        entityManager.persist(record);
        DiagnosisResult diagnosis = DiagnosisResult.builder().user(user).sessionId("survey")
                .type(DiagnosisType.SIGNUP).score(4).level(CallPhobiaLevel.LEVEL_4).summary("요약")
                .questionnaireVersion("SIGNUP_V1")
                .answerSnapshot(List.of(new DiagnosisResult.AnswerSnapshot(1L, "전화가 불안한가요", 1, 4))).build();
        entityManager.persist(diagnosis);
        entityManager.flush(); entityManager.clear();
        assertThat(entityManager.find(TrainingRecord.class, record.getRecordId()).getPerformanceScore()).isEqualByComparingTo("100.0000");
        assertThat(entityManager.find(DiagnosisResult.class, diagnosis.getQuestionId()).getAnswerSnapshot())
                .containsExactly(new DiagnosisResult.AnswerSnapshot(1L, "전화가 불안한가요", 1, 4));
    }

    @Test
    void frozenPolicyAndResultsCanBeLoadedAndInvalidatedByNativeUserLookup() {
        var policy = new ObjectMapper().findAndRegisterModules().convertValue(Map.of(), EvaluationPolicy.class);
        var result = new EvaluationResult(7L, Set.of("BETA"), LocalDateTime.now(), null, 0, 0, List.of(), "UNREACHED",
                Map.of(EvaluationPolicy.Metric.SELF_ASSESSMENT, EvaluationResult.MetricResult.unavailable("MISSING_BASELINE")));
        EvaluationRun run = runs.saveAndFlush(new EvaluationRun("베타", 1L, LocalDateTime.now(), "V1", policy, List.of(result), Map.of()));
        entityManager.clear();
        EvaluationRun loaded = runs.findById(run.getId()).orElseThrow();
        assertThat(loaded.getPolicy()).isEqualTo(policy);
        assertThat(loaded.getResults()).containsExactly(result);
        assertThat(runs.findContainingUserForUpdate(7L)).hasSize(1);
        new EvaluationSourceRemovalListener(runs, cohorts).removed(new TrainingDataRemoved(7L, null));
        entityManager.flush(); entityManager.clear();
        assertThat(runs.findById(run.getId()).orElseThrow().isInvalidated()).isTrue();
        assertThat(runs.findById(run.getId()).orElseThrow().getResults()).isEmpty();
    }

    @Test
    void actualRepositoriesExecuteAndExportSelectedAccountSurveyComparison() {
        Users user = user();
        LocalDateTime start = LocalDateTime.now().minusDays(1).minusMinutes(1);
        DiagnosisResult before = DiagnosisResult.builder().user(user).sessionId("before").type(DiagnosisType.SIGNUP)
                .score(4).level(CallPhobiaLevel.LEVEL_4).questionnaireVersion("V1").questionnaireVersionConfirmed(true).build();
        DiagnosisResult after = DiagnosisResult.builder().user(user).sessionId("after").type(DiagnosisType.SIGNUP)
                .score(3).level(CallPhobiaLevel.LEVEL_3).questionnaireVersion("V1").questionnaireVersionConfirmed(true).build();
        entityManager.persist(before); entityManager.persist(after);
        org.springframework.test.util.ReflectionTestUtils.setField(before, "createdAt", start);
        entityManager.flush();
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        var policy = mapper.convertValue(Map.of("targetCount", 1, "metrics", List.of("SELF_ASSESSMENT")), EvaluationPolicy.class);
        AdminEvaluationService service = new AdminEvaluationService(users, cohorts, runs, diagnosesRepository,
                records, new EffectivenessCalculator(), mapper);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getUsername(), null));
        try {
            var request = new EvaluationRequest("실제 DB 평가", Set.of(user.getUserId()), Set.of(), Map.of(),
                    Map.of(user.getUserId(), new EvaluationRequest.ParticipantOverride("BETA", start, null, null, null)), null, policy);
            var result = service.execute(request);
            assertThat(result.summary().metrics().get(EvaluationPolicy.Metric.SELF_ASSESSMENT).meanChange()).isEqualByComparingTo("1");
            assertThat(new String(service.export(result.id()).content(), java.nio.charset.StandardCharsets.UTF_8)).contains("SELF_ASSESSMENT", "IMPROVED");
        } finally { SecurityContextHolder.clearContext(); }
    }

    @Test
    void replacingSameCohortMemberDoesNotViolateUniqueConstraint() {
        EvaluationCohort cohort = cohorts.saveAndFlush(new EvaluationCohort("테스트"));
        cohort.replaceParticipants(List.of(new EvaluationParticipant(cohort, 7L, "BETA", LocalDateTime.now(), null)));
        entityManager.flush();
        cohort.replaceParticipants(List.of()); entityManager.flush();
        cohort.replaceParticipants(List.of(new EvaluationParticipant(cohort, 7L, "ORGANIC", LocalDateTime.now(), null)));
        entityManager.flush(); entityManager.clear();
        assertThat(cohorts.findById(cohort.getId()).orElseThrow().getParticipants()).hasSize(1);
        assertThat(cohorts.findById(cohort.getId()).orElseThrow().getParticipants().getFirst().getSource()).isEqualTo("ORGANIC");
    }

    private Users user() {
        Users user = Users.builder().username("evaluation-test").name("테스트").build();
        entityManager.persist(user); return user;
    }
}
