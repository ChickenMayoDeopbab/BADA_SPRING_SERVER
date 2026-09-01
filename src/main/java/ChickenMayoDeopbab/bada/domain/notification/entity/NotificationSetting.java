package ChickenMayoDeopbab.bada.domain.notification.entity;

import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification_settings")
public class NotificationSetting {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users user;

    @Column(nullable = false)
    private boolean allEnabled;

    @Column(nullable = false)
    private boolean communityEnabled;

    @Column(nullable = false)
    private boolean trainingEnabled;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private NotificationSetting(Users user) {
        this.user = user;
        this.allEnabled = true;
        this.communityEnabled = true;
        this.trainingEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public static NotificationSetting enabledByDefault(Users user) {
        return new NotificationSetting(user);
    }

    public boolean allowsTrainingNotification() {
        return allEnabled && trainingEnabled;
    }

    public boolean allowsCommunityNotification() {
        return allEnabled && communityEnabled;
    }

    public void update(
            boolean allEnabled,
            boolean communityEnabled,
            boolean trainingEnabled
    ) {
        this.allEnabled = allEnabled;
        this.communityEnabled = communityEnabled;
        this.trainingEnabled = trainingEnabled;
        this.updatedAt = LocalDateTime.now();
    }
}
