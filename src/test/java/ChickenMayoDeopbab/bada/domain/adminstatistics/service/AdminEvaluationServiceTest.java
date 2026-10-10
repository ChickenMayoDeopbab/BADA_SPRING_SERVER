package ChickenMayoDeopbab.bada.domain.adminstatistics.service;

import ChickenMayoDeopbab.bada.domain.adminstatistics.dto.request.EvaluationRequest;
import ChickenMayoDeopbab.bada.domain.adminstatistics.entity.*;
import ChickenMayoDeopbab.bada.domain.adminstatistics.model.*;
import ChickenMayoDeopbab.bada.domain.adminstatistics.repository.*;
import ChickenMayoDeopbab.bada.domain.diagnosis.repository.DiagnosisResultRepository;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminEvaluationServiceTest {
    private final UsersRepository users = mock(UsersRepository.class);
    private final EvaluationCohortRepository cohorts = mock(EvaluationCohortRepository.class);
    private final EvaluationRunRepository runs = mock(EvaluationRunRepository.class);
    private final DiagnosisResultRepository diagnoses = mock(DiagnosisResultRepository.class);
    private final TrainingRecordRepository training = mock(TrainingRecordRepository.class);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final EvaluationPolicy policy = mapper.convertValue(Map.of(), EvaluationPolicy.class);
    private final AdminEvaluationService service = new AdminEvaluationService(users, cohorts, runs, diagnoses,
            training, new EffectivenessCalculator(), mapper);
    private final LocalDateTime start = LocalDateTime.of(2026, 9, 1, 0, 0);

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null, List.of()));
        when(users.findByUsername("admin")).thenReturn(Optional.of(Users.builder().userId(99L).build()));
        when(users.findAllById(any())).thenAnswer(invocation -> {
            Iterable<Long> ids = invocation.getArgument(0);
            List<Users> result = new ArrayList<>(); ids.forEach(id -> result.add(Users.builder().userId(id).build())); return result;
        });
        when(runs.save(any())).thenAnswer(invocation -> {
            EvaluationRun run = invocation.getArgument(0); ReflectionTestUtils.setField(run, "id", 1L); return run;
        });
    }
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void deduplicatesCohortsAndManualSelectionWhileKeepingBothSources() {
        when(cohorts.findById(1L)).thenReturn(Optional.of(cohort("베타", "BETA")));
        when(cohorts.findById(2L)).thenReturn(Optional.of(cohort("일반", "ORGANIC")));
        var result = service.execute(request(Set.of(7L), Set.of(1L, 2L), Map.of(), Map.of()));
        assertThat(result.summary().selectedUsers()).isEqualTo(1);
        assertThat(result.participants().getFirst().sources()).containsExactlyInAnyOrder("BETA", "ORGANIC");
        assertThat(result.bySource()).containsKeys("BETA", "ORGANIC");
    }

    @Test
    void explicitExclusionWinsAndRequiresReason() {
        var result = service.execute(request(Set.of(7L, 8L), Set.of(), Map.of(8L, "내부 테스트 계정"), Map.of()));
        assertThat(result.participants()).extracting(EvaluationResult::userId).containsExactly(7L);
        assertThat(result.exclusions()).containsEntry(8L, "내부 테스트 계정");
        assertThatThrownBy(() -> service.execute(request(Set.of(7L), Set.of(), Map.of(7L, ""), Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptySelectionNeverBecomesAllUsers() {
        assertThatThrownBy(() -> service.execute(request(Set.of(), Set.of(), Map.of(), Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
        verify(runs, never()).save(any()); verifyNoInteractions(training, diagnoses);
    }

    @Test
    void selectedAccountWithoutStateOrMeasurementsRemainsVisible() {
        var result = service.execute(request(Set.of(7L), Set.of(), Map.of(), Map.of()));
        assertThat(result.summary().selectedUsers()).isEqualTo(1);
        assertThat(result.participants().getFirst().metrics().values()).allMatch(value -> "MISSING_ANCHOR".equals(value.reason()));
    }

    @Test
    void foreignOrFutureAssessmentIdsAreRejected() {
        var override = new EvaluationRequest.ParticipantOverride("BETA", start, null, 123L, null);
        assertThatThrownBy(() -> service.execute(request(Set.of(7L), Set.of(), Map.of(), Map.of(7L, override))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("해당 계정 소유");
        verify(runs, never()).save(any());
    }

    @Test
    void deletedSourcesInvalidateSavedRunAndPreventExport() {
        EvaluationResult result = new EvaluationResult(7L, Set.of("BETA"), start, start.plusDays(14), 1, 1,
                List.of(123L), "ELAPSED_DAYS:20160min", Map.of(
                EvaluationPolicy.Metric.SELF_ASSESSMENT, EvaluationResult.MetricResult.unavailable("MISSING_BASELINE"),
                EvaluationPolicy.Metric.SUBJECTIVE_ANXIETY, EvaluationResult.MetricResult.unavailable("INSUFFICIENT_NON_OVERLAPPING_OBSERVATIONS")));
        EvaluationRun run = new EvaluationRun("베타", 99L, LocalDateTime.now(), "V1", policy, List.of(result), Map.of());
        when(runs.findById(1L)).thenReturn(Optional.of(run));
        assertThatThrownBy(() -> service.export(1L)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("원본이 삭제");
        assertThat(run.isInvalidated()).isTrue(); assertThat(run.getResults()).isEmpty();
    }

    @Test
    void policyHashIsStableAcrossInputSetOrder() {
        var first = service.execute(request(Set.of(7L), Set.of(), Map.of(), Map.of()));
        EvaluationPolicy reversed = mapper.convertValue(Map.of("metrics", List.of("SUBJECTIVE_ANXIETY", "SELF_ASSESSMENT")), EvaluationPolicy.class);
        var second = service.execute(new EvaluationRequest("두 번째", Set.of(7L), Set.of(), Map.of(), Map.of(), null, reversed));
        assertThat(second.policyVersion()).isEqualTo(first.policyVersion());
    }

    private EvaluationCohort cohort(String name, String source) {
        EvaluationCohort cohort = new EvaluationCohort(name);
        cohort.replaceParticipants(List.of(new EvaluationParticipant(cohort, 7L, source, start, null))); return cohort;
    }
    private EvaluationRequest request(Set<Long> ids, Set<Long> cohortIds, Map<Long, String> excluded,
            Map<Long, EvaluationRequest.ParticipantOverride> overrides) {
        return new EvaluationRequest("효과 평가", ids, cohortIds, excluded, overrides, null, policy);
    }
}
