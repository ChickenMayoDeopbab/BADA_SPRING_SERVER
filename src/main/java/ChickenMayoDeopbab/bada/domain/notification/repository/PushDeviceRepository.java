package ChickenMayoDeopbab.bada.domain.notification.repository;

import ChickenMayoDeopbab.bada.domain.notification.entity.PushDevice;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PushDeviceRepository extends JpaRepository<PushDevice, Long> {

    Optional<PushDevice> findByInstallationId(String installationId);

    Optional<PushDevice> findByToken(String token);

    void deleteByInstallationIdAndUser(String installationId, Users user);
}
