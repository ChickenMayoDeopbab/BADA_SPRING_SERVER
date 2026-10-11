package ChickenMayoDeopbab.bada.domain.adminstatistics.repository;

import ChickenMayoDeopbab.bada.domain.adminstatistics.entity.EvaluationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface EvaluationRunRepository extends JpaRepository<EvaluationRun, Long> {
    @Query(value = "select * from evaluation_runs where results @> jsonb_build_array(jsonb_build_object('userId', cast(:userId as bigint))) "
            + "or jsonb_exists(exclusions, cast(:userId as text)) order by id for update", nativeQuery = true)
    List<EvaluationRun> findContainingUserForUpdate(Long userId);
}
