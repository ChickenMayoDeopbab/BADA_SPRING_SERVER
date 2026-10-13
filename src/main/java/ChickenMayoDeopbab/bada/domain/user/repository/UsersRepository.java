package ChickenMayoDeopbab.bada.domain.user.repository;

import ChickenMayoDeopbab.bada.domain.user.entity.Provider;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface UsersRepository extends JpaRepository<Users, Long> {
    @Query("select user from Users user where lower(user.username) like lower(concat('%', :term, '%')) "
            + "or lower(user.name) like lower(concat('%', :term, '%')) "
            + "or lower(user.email) like lower(concat('%', :term, '%'))")
    Page<Users> searchForEvaluation(String term, Pageable pageable);
    Optional<Users> findByUsername(String username);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from Users user where user.username = :username")
    Optional<Users> findByUsernameForDiagnosisUpdate(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from Users user where user.username = :username")
    Optional<Users> findByUsernameForRingtoneUpdate(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    Optional<Users> findByEmailAndProvider(String email, Provider provider);

    Optional<Users> findByProviderAndProviderId(Provider provider, String providerId);

    Optional<Users> findByEmail(String email);
}
