package ChickenMayoDeopbab.bada.domain.diagnosis.repository;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisQuestion;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiagnosisRepository extends JpaRepository<DiagnosisQuestion, Long> {
    List<DiagnosisQuestion> findByTypeOrderByOrderIndex(DiagnosisType type);
}
