package ChickenMayoDeopbab.bada.domain.adminstatistics.dto.request;

import ChickenMayoDeopbab.bada.domain.adminstatistics.model.EvaluationPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record EvaluationRequest(@NotBlank @Size(max = 100) String name,
        Set<@NotNull Long> userIds, Set<@NotNull Long> cohortIds, Map<@NotNull Long, @NotBlank String> excludedUsers,
        @Valid Map<@NotNull Long, @NotNull ParticipantOverride> participantOverrides, LocalDateTime asOf,
        @NotNull @Valid EvaluationPolicy policy) {
    public record ParticipantOverride(String source, LocalDateTime startedAt, LocalDateTime endOverride,
            Long baselineAssessmentId, Long postAssessmentId) {}
    public record CohortRequest(@NotBlank @Size(max = 100) String name) {}
    public record MemberRequest(@NotNull Long userId, String source, LocalDateTime startedAt, LocalDateTime endOverride) {}
    public record MembersRequest(@NotNull @Valid List<@NotNull MemberRequest> members) {}
}
