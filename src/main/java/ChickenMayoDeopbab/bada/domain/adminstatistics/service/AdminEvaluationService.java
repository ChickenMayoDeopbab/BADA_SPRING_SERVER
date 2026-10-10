package ChickenMayoDeopbab.bada.domain.adminstatistics.service;

import ChickenMayoDeopbab.bada.domain.adminstatistics.dto.request.EvaluationRequest;
import ChickenMayoDeopbab.bada.domain.adminstatistics.entity.*;
import ChickenMayoDeopbab.bada.domain.adminstatistics.model.*;
import ChickenMayoDeopbab.bada.domain.adminstatistics.repository.*;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisResult;
import ChickenMayoDeopbab.bada.domain.diagnosis.repository.DiagnosisResultRepository;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminEvaluationService {
    private final UsersRepository users;
    private final EvaluationCohortRepository cohorts;
    private final EvaluationRunRepository runs;
    private final DiagnosisResultRepository diagnoses;
    private final TrainingRecordRepository training;
    private final EffectivenessCalculator calculator;
    private final ObjectMapper mapper;

    public record Account(Long userId, String username, String name, String email) {}
    public record Member(Long userId, String source, LocalDateTime startedAt, LocalDateTime endOverride) {}
    public record Cohort(Long id, String name, List<Member> members) {}
    public record Run(Long id, String name, String policyVersion, LocalDateTime createdAt, LocalDateTime asOf,
            boolean invalidated, EvaluationPolicy policy, Map<Long, String> exclusions,
            EvaluationResult.Summary summary, Map<String, EvaluationResult.Summary> bySource,
            List<EvaluationResult> participants) {}

    public Page<Account> accounts(String term, int page, int size) {
        if (page < 0 || size < 1 || size > 100) { throw new IllegalArgumentException("페이지 크기는 1~100이어야 합니다."); }
        return users.searchForEvaluation(term, PageRequest.of(page, size, Sort.by("userId"))).map(user ->
                new Account(user.getUserId(), user.getUsername(), user.getName(), user.getEmail()));
    }
    public List<Cohort> cohorts() {
        return cohorts.findAll(Sort.by("id")).stream().map(this::view).toList();
    }
    public record Assessment(Long id, String type, double score, String questionnaireVersion,
            Boolean questionnaireVersionConfirmed, LocalDateTime assessedAt, List<DiagnosisResult.AnswerSnapshot> answers) {}
    public Page<Assessment> assessments(Long userId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) { throw new IllegalArgumentException("페이지 크기는 1~100이어야 합니다."); }
        requireAccounts(Set.of(userId));
        return diagnoses.findByUserUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)).map(result ->
                new Assessment(result.getQuestionId(), result.getType().name(), result.getScore(), result.getQuestionnaireVersion(),
                        result.getQuestionnaireVersionConfirmed(), result.getCreatedAt(), result.getAnswerSnapshot()));
    }
    @Transactional
    public Cohort createCohort(String name) { return view(cohorts.save(new EvaluationCohort(name))); }

    @Transactional
    public Cohort replaceMembers(Long cohortId, List<EvaluationRequest.MemberRequest> members) {
        EvaluationCohort cohort = cohorts.findForUpdate(cohortId).orElseThrow(() -> new IllegalArgumentException("평가 명단이 없습니다."));
        Set<Long> ids = members.stream().map(EvaluationRequest.MemberRequest::userId).collect(Collectors.toSet());
        if (ids.size() != members.size()) { throw new IllegalArgumentException("명단에 중복 계정이 있습니다."); }
        requireAccounts(ids);
        // 기존 행을 먼저 제거해야 같은 계정의 재등록이 유일 제약과 충돌하지 않는다.
        cohort.replaceParticipants(List.of());
        cohorts.flush();
        cohort.replaceParticipants(members.stream().map(member -> {
            if (member.endOverride() != null && member.startedAt() != null && !member.endOverride().isAfter(member.startedAt())) {
                throw new IllegalArgumentException("개인별 종료는 시작 이후여야 합니다.");
            }
            return new EvaluationParticipant(cohort, member.userId(), source(member.source()), member.startedAt(), member.endOverride());
        }).toList());
        return view(cohort);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Run execute(EvaluationRequest request) {
        LocalDateTime asOf = request.asOf() == null ? LocalDateTime.now() : request.asOf();
        if (asOf.isAfter(LocalDateTime.now())) { throw new IllegalArgumentException("미래 시점으로 평가할 수 없습니다."); }
        Map<Long, EffectivenessCalculator.Participant> selected = new TreeMap<>();
        Set<Long> cohortIds = request.cohortIds() == null ? Set.of() : request.cohortIds();
        for (Long id : cohortIds) {
            EvaluationCohort cohort = cohorts.findById(id).orElseThrow(() -> new IllegalArgumentException("평가 명단이 없습니다."));
            for (EvaluationParticipant member : cohort.getParticipants()) {
                EffectivenessCalculator.Participant previous = selected.get(member.getUserId());
                if (previous != null && (!Objects.equals(previous.startedAt(), member.getStartedAt())
                        || !Objects.equals(previous.endOverride(), member.getEndOverride()))
                        && (request.participantOverrides() == null || !request.participantOverrides().containsKey(member.getUserId()))) {
                    throw new IllegalArgumentException("중복 명단의 참여 기간이 다릅니다. 개인별 기간을 지정해 주세요.");
                }
                Set<String> sources = new TreeSet<>(previous == null ? Set.of() : previous.sources()); sources.add(member.getSource());
                selected.put(member.getUserId(), new EffectivenessCalculator.Participant(member.getUserId(), Set.copyOf(sources),
                        member.getStartedAt(), member.getEndOverride(), null, null));
            }
        }
        if (request.userIds() != null) {
            request.userIds().forEach(id -> selected.putIfAbsent(id, new EffectivenessCalculator.Participant(id, Set.of("UNKNOWN"), null, null, null, null)));
        }
        Map<Long, String> exclusions = request.excludedUsers() == null ? Map.of() : Map.copyOf(request.excludedUsers());
        exclusions.forEach((id, reason) -> {
            if (!selected.containsKey(id) || reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("제외 대상은 선택한 계정이어야 하며 사유가 필요합니다.");
            }
        });
        exclusions.keySet().forEach(selected::remove);
        if (selected.isEmpty()) { throw new IllegalArgumentException("평가할 계정을 선택해 주세요."); }
        requireAccounts(selected.keySet());
        if (request.participantOverrides() != null) {
            request.participantOverrides().forEach((id, override) -> {
                EffectivenessCalculator.Participant previous = selected.get(id);
                if (previous == null) { throw new IllegalArgumentException("개인별 설정은 평가 대상 계정에만 지정할 수 있습니다."); }
                selected.put(id, new EffectivenessCalculator.Participant(id,
                        override.source() == null ? previous.sources() : Set.of(source(override.source())),
                        override.startedAt() == null ? previous.startedAt() : override.startedAt(),
                        override.endOverride() == null ? previous.endOverride() : override.endOverride(),
                        override.baselineAssessmentId(), override.postAssessmentId()));
            });
        }
        List<DiagnosisResult> assessmentRows = diagnoses.findForEvaluation(selected.keySet(), asOf);
        Map<Long, List<DiagnosisResult>> assessments = assessmentRows.stream().collect(Collectors.groupingBy(row -> row.getUser().getUserId()));
        Map<Long, List<TrainingRecord>> records = training.findForEvaluation(selected.keySet(), asOf).stream()
                .collect(Collectors.groupingBy(row -> row.getUser().getUserId()));
        if (request.participantOverrides() != null) {
            request.participantOverrides().forEach((id, override) -> {
                Set<Long> owned = assessments.getOrDefault(id, List.of()).stream().map(DiagnosisResult::getQuestionId).collect(Collectors.toSet());
                if ((override.baselineAssessmentId() != null && !owned.contains(override.baselineAssessmentId()))
                        || (override.postAssessmentId() != null && !owned.contains(override.postAssessmentId()))) {
                    throw new IllegalArgumentException("검사는 해당 계정 소유이며 집계 시점 이전의 자료여야 합니다.");
                }
            });
        }
        List<EvaluationResult> results = selected.values().stream().map(participant -> calculator.evaluate(request.policy(), participant,
                asOf, assessments.getOrDefault(participant.userId(), List.of()), records.getOrDefault(participant.userId(), List.of()))).toList();
        Users administrator = users.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow();
        EvaluationRun run = runs.save(new EvaluationRun(request.name(), administrator.getUserId(), asOf,
                policyVersion(request.policy()), request.policy(), results, exclusions));
        return view(run);
    }

    @Transactional
    public Run run(Long id) {
        EvaluationRun run = runs.findById(id).orElseThrow(() -> new IllegalArgumentException("평가 실행이 없습니다."));
        // 실행 완료 직전의 원본 삭제와 이벤트 전달이 겹친 경우에도 조회 시 다시 확인한다.
        Set<Long> selected = run.getResults().stream().map(EvaluationResult::userId).collect(Collectors.toSet());
        if (!selected.isEmpty() && !run.isInvalidated()) {
            Set<Long> accounts = users.findAllById(selected).stream().map(Users::getUserId).collect(Collectors.toSet());
            Set<Long> recordIds = training.findForEvaluation(selected, LocalDateTime.now()).stream()
                    .map(TrainingRecord::getRecordId).collect(Collectors.toSet());
            Set<Long> assessmentIds = diagnoses.findForEvaluation(selected, LocalDateTime.now()).stream()
                    .map(DiagnosisResult::getQuestionId).collect(Collectors.toSet());
            for (EvaluationResult result : List.copyOf(run.getResults())) {
                boolean removed = !accounts.contains(result.userId()) || !recordIds.containsAll(result.trainingRecordIds())
                        || result.metrics().entrySet().stream().anyMatch(entry ->
                        !(entry.getKey() == EvaluationPolicy.Metric.SELF_ASSESSMENT ? assessmentIds : recordIds)
                                .containsAll(entry.getValue().sourceIds()));
                if (removed) { run.eraseParticipant(result.userId()); }
            }
        }
        return view(run);
    }
    public record RunHeader(Long id, String name, String policyVersion, LocalDateTime createdAt, boolean invalidated) {}
    public Page<RunHeader> runs(int page, int size) {
        if (page < 0 || size < 1 || size > 100) { throw new IllegalArgumentException("페이지 크기는 1~100이어야 합니다."); }
        return runs.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))).map(run ->
                new RunHeader(run.getId(), run.getName(), run.getPolicyVersion(), run.getCreatedAt(), run.isInvalidated()));
    }
    @Transactional
    public CallAnxietyCsvExport export(Long id) {
        Run run = run(id);
        if (run.invalidated()) { throw new IllegalArgumentException("원본이 삭제된 평가입니다. 새로 실행해 주세요."); }
        StringBuilder csv = new StringBuilder("\uFEFFuserId,sources,metric,status,reason,before,after,change,direction,meetsThreshold,beforeCount,afterCount,sourceIds,versions,trainingDays,trainingSessions,policyVersion,asOf\r\n");
        run.participants().forEach(participant -> participant.metrics().forEach((metric, value) -> {
            List<Object> fields = Arrays.asList(participant.userId(), participant.sources(), metric, value.status(), value.reason(),
                    value.before(), value.after(), value.change(), value.direction(), value.meetsThreshold(), value.beforeCount(),
                    value.afterCount(), value.sourceIds(), value.versions(), participant.trainingDays(), participant.trainingSessions(),
                    run.policyVersion(), run.asOf());
            csv.append(fields.stream().map(field -> quote(field == null ? "" : field.toString())).collect(Collectors.joining(","))).append("\r\n");
        }));
        return new CallAnxietyCsvExport("effectiveness-" + id + ".csv", csv.toString().getBytes(StandardCharsets.UTF_8));
    }
    private String quote(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
    private Cohort view(EvaluationCohort cohort) {
        return new Cohort(cohort.getId(), cohort.getName(), cohort.getParticipants().stream()
                .map(member -> new Member(member.getUserId(), member.getSource(), member.getStartedAt(), member.getEndOverride())).toList());
    }
    private Run view(EvaluationRun run) {
        Map<String, EvaluationResult.Summary> bySource = new TreeMap<>();
        run.getResults().stream().flatMap(result -> result.sources().stream()).distinct().forEach(source -> bySource.put(source,
                calculator.summarize(run.getResults().stream().filter(result -> result.sources().contains(source)).toList(), run.getPolicy().metrics())));
        return new Run(run.getId(), run.getName(), run.getPolicyVersion(), run.getCreatedAt(), run.getAsOf(), run.isInvalidated(),
                run.getPolicy(), run.getExclusions(), run.isInvalidated() ? null : calculator.summarize(run.getResults(), run.getPolicy().metrics()),
                run.isInvalidated() ? Map.of() : bySource, run.getResults());
    }
    private void requireAccounts(Set<Long> ids) {
        if (ids.stream().anyMatch(Objects::isNull) || users.findAllById(ids).size() != ids.size()) {
            throw new IllegalArgumentException("존재하지 않는 계정이 포함되어 있습니다.");
        }
    }
    private String source(String source) {
        String value = source == null ? "UNKNOWN" : source;
        if (!Set.of("BETA", "ORGANIC", "UNKNOWN").contains(value)) { throw new IllegalArgumentException("유입 분류는 BETA, ORGANIC, UNKNOWN 중 하나여야 합니다."); }
        return value;
    }
    private String policyVersion(EvaluationPolicy policy) {
        try {
            var tree = canonical(mapper.valueToTree(policy));
            return "EFFECTIVENESS_V1_" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(mapper.writer().with(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                            .writeValueAsBytes(tree)));
        } catch (NoSuchAlgorithmException | JsonProcessingException exception) { throw new IllegalStateException(exception); }
    }
    private com.fasterxml.jackson.databind.JsonNode canonical(com.fasterxml.jackson.databind.JsonNode node) {
        if (node.isObject()) {
            var result = mapper.createObjectNode();
            List<String> names = new ArrayList<>(); node.fieldNames().forEachRemaining(names::add);
            names.stream().sorted().forEach(name -> result.set(name, canonical(node.get(name))));
            return result;
        }
        if (node.isArray()) {
            List<com.fasterxml.jackson.databind.JsonNode> values = new ArrayList<>();
            node.forEach(value -> values.add(canonical(value)));
            values.sort(Comparator.comparing(com.fasterxml.jackson.databind.JsonNode::toString));
            var result = mapper.createArrayNode(); values.forEach(result::add); return result;
        }
        return node;
    }
}
