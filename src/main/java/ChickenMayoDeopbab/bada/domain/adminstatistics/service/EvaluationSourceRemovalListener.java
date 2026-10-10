package ChickenMayoDeopbab.bada.domain.adminstatistics.service;

import ChickenMayoDeopbab.bada.domain.adminstatistics.repository.EvaluationRunRepository;
import ChickenMayoDeopbab.bada.domain.adminstatistics.repository.EvaluationCohortRepository;
import ChickenMayoDeopbab.bada.domain.trainingrecord.model.TrainingDataRemoved;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EvaluationSourceRemovalListener {
    private final EvaluationRunRepository runs;
    private final EvaluationCohortRepository cohorts;

    // 원본 삭제와 같은 트랜잭션에서 파생 응답을 제거해 삭제 자료가 내보내기에 남지 않게 한다.
    @EventListener
    public void removed(TrainingDataRemoved event) {
        runs.findContainingUserForUpdate(event.userId()).forEach(run -> {
            boolean affected = event.recordId() == null || run.getResults().stream()
                    .anyMatch(result -> result.userId().equals(event.userId()) && result.trainingRecordIds().contains(event.recordId()));
            if (affected) { run.eraseParticipant(event.userId()); }
        });
        if (event.recordId() == null) { cohorts.removeParticipantFromAllCohorts(event.userId()); }
    }
}
