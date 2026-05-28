package ChickenMayoDeopbab.bada.domain.user.repository;

import ChickenMayoDeopbab.bada.domain.user.entity.Provider;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    Optional<Users> findByEmailAndProvider(String email, Provider provider);

    Optional<Users> findByEmail(String email);
}
