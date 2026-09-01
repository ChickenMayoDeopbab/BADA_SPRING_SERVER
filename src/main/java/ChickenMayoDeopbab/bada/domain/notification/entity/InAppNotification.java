package ChickenMayoDeopbab.bada.domain.notification.entity;

import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "in_app_notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_in_app_notifications_event_key",
                columnNames = "event_key"
        ),
        indexes = {
                @Index(
                        name = "idx_in_app_notifications_recipient_created",
                        columnList = "recipient_user_id, created_at"
                ),
                @Index(
                        name = "idx_in_app_notifications_recipient_read_created",
                        columnList = "recipient_user_id, read_at, created_at"
                )
        }
)
public class InAppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InAppNotificationType type;

    private Long actorUserId;

    @Column(length = 50)
    private String actorName;

    private String actorProfileImage;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 255)
    private String message;

    private Long postId;

    private Long commentId;

    private Long scheduleId;

    @Column(name = "event_key", nullable = false, length = 150)
    private String eventKey;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    private InAppNotification(
            Users recipient,
            InAppNotificationType type,
            Long actorUserId,
            String actorName,
            String actorProfileImage,
            String title,
            String message,
            Long postId,
            Long commentId,
            Long scheduleId,
            String eventKey
    ) {
        this.recipient = recipient;
        this.type = type;
        this.actorUserId = actorUserId;
        this.actorName = actorName;
        this.actorProfileImage = actorProfileImage;
        this.title = title;
        this.message = message;
        this.postId = postId;
        this.commentId = commentId;
        this.scheduleId = scheduleId;
        this.eventKey = eventKey;
        this.createdAt = LocalDateTime.now();
    }

    public static InAppNotification create(
            Users recipient,
            InAppNotificationType type,
            Long actorUserId,
            String actorName,
            String actorProfileImage,
            String title,
            String message,
            Long postId,
            Long commentId,
            Long scheduleId,
            String eventKey
    ) {
        return new InAppNotification(
                recipient,
                type,
                actorUserId,
                actorName,
                actorProfileImage,
                title,
                message,
                postId,
                commentId,
                scheduleId,
                eventKey
        );
    }

    public boolean isRead() {
        return readAt != null;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }
}
