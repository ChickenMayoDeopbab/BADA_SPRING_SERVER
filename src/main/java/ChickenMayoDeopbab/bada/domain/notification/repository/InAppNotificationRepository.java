package ChickenMayoDeopbab.bada.domain.notification.repository;

import ChickenMayoDeopbab.bada.domain.notification.entity.InAppNotification;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    void deleteAllByRecipient(Users recipient);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update InAppNotification notification
            set notification.actorUserId = null,
                notification.actorName = null,
                notification.actorProfileImage = null
            where notification.actorUserId = :userId
            """)
    void anonymizeActorByUserId(@Param("userId") Long userId);
}
