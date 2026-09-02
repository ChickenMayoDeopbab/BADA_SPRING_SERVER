package ChickenMayoDeopbab.bada.domain.diagnosis.repository;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisResult;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DiagnosisResultRepository extends JpaRepository<DiagnosisResult, Integer> {
    Optional<DiagnosisResult> findFirstByUserOrderByCreatedAtDesc(Users user);

    @Modifying(flushAutomatically = true)
    @Query("delete from DiagnosisResult d where d.user = :user")
    void deleteAllByUser(@Param("user") Users user);
}
