package ChickenMayoDeopbab.bada.domain.trainingrecord.repository;

import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request.TrainingRecordFilter;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingSpeechMetrics;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.global.config.QueryDslConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 로컬의 폐기 가능한 dashboard_test DB에서만 JSONB 매핑과 실제 조회를 검증한다. */
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig.class)
@EnabledIfEnvironmentVariable(named = "DASHBOARD_TEST_DATABASE_URL", matches = ".+")
class TrainingRecordDashboardQueriesTest {
    @Autowired private TrainingRecordRepository records;
    @Autowired private TestEntityManager entityManager;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = System.getenv("DASHBOARD_TEST_DATABASE_URL");
        if (!url.matches("jdbc:postgresql://(127\\.0\\.0\\.1|localhost):[0-9]+/dashboard_test")) {
            throw new IllegalArgumentException("폐기 가능한 로컬 dashboard_test DB가 필요합니다.");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> "dashboard_test");
        registry.add("spring.datasource.password", () -> "");
    }

    @Test
    void loads_jsonb_projection_with_owner_date_and_condition_filters() {
        Users owner = user("owner"), other = user("other");
        var start = LocalDateTime.of(2026, 10, 1, 0, 0);
        var summary = new TrainingSpeechMetrics(1, "FEEDBACK_METRICS_V1", "FEEDBACK_POLICY_V1",
                Map.of("tremor_turn_rate", new TrainingSpeechMetrics.Measurement("MEASURED", 5, 0, null)));
        var expected = record(owner, start, "HARD", summary);
        record(owner, start.minusSeconds(1), "HARD", summary);
        record(owner, start.plusDays(1), "HARD", summary);
        record(owner, start, "EASY", summary);
        record(other, start, "HARD", summary);
        entityManager.flush();
        entityManager.clear();

        var filter = new TrainingRecordFilter(SessionType.SCENARIO, 12L, "SCENARIO_V1", "HARD", AiPersonality.KIND, 3);
        var rows = records.findDashboardRows(owner, start, start.plusDays(1), filter);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().recordId()).isEqualTo(expected.getRecordId());
        assertThat(rows.getFirst().speechMetrics()).isEqualTo(summary);
        assertThat(rows.getFirst().speechMetrics().metrics().get("tremor_turn_rate").hasValidRate()).isTrue();

        var first = records.findFirstScenarioTrainings(owner, List.of(12L), Set.of(EndReason.END_CALL));
        assertThat(first).hasSize(1);
        assertThat(first.getFirst().startedAt()).isEqualTo(start.minusSeconds(1));
    }

    @Test
    void loads_legacy_records_without_a_speech_summary() {
        Users owner = user("legacy");
        var start = LocalDateTime.of(2026, 10, 1, 0, 0);
        record(owner, start, "EASY", null);
        entityManager.flush();
        entityManager.clear();
        var rows = records.findDashboardRows(owner, start, start.plusDays(1),
                new TrainingRecordFilter(null, null, null, null, null, null));
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().speechMetrics()).isNull();
    }

    private Users user(String name) {
        var now = LocalDateTime.now();
        return entityManager.persist(Users.builder().username(name).createdAt(now).updatedAt(now).build());
    }

    private TrainingRecord record(Users user, LocalDateTime start, String difficulty, TrainingSpeechMetrics summary) {
        return entityManager.persist(TrainingRecord.builder().user(user).sessionId(UUID.randomUUID().toString())
                .scenarioId(12L).scenarioName("예약").scenarioVersion("SCENARIO_V1").difficulty(difficulty)
                .scriptLevel(3).sessionType(SessionType.SCENARIO).aiPersonality(AiPersonality.KIND)
                .endReason(EndReason.END_CALL).startedAt(start).endedAt(start.plusSeconds(60)).durationSeconds(60L)
                .speechMetrics(summary).build());
    }
}
