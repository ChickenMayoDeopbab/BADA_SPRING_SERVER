package ChickenMayoDeopbab.bada.domain.diagnosis.repository;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisResult;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiagnosisResultRepository extends JpaRepository<DiagnosisResult, Integer> {
    Optional<DiagnosisResult> findByUser(Users user);
}
