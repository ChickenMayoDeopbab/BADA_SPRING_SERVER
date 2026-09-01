package ChickenMayoDeopbab.bada.domain.notification.repository;

import ChickenMayoDeopbab.bada.domain.notification.entity.InAppNotification;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

    Page<InAppNotification> findByRecipientAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Users recipient,
            LocalDateTime createdAfter,
            Pageable pageable
    );

    Page<InAppNotification> findByRecipientAndReadAtIsNullAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Users recipient,
            LocalDateTime createdAfter,
            Pageable pageable
    );

    long countByRecipientAndReadAtIsNullAndCreatedAtGreaterThanEqual(
            Users recipient,
            LocalDateTime createdAfter
    );

    Optional<InAppNotification> findByNotificationIdAndRecipient(
            Long notificationId,
            Users recipient
    );

    boolean existsByEventKey(String eventKey);
}
