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
        name = "push_devices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_push_devices_installation_id", columnNames = "installation_id"),
                @UniqueConstraint(name = "uk_push_devices_token", columnNames = "token")
        },
        indexes = @Index(name = "idx_push_devices_user_id", columnList = "user_id")
)
public class PushDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pushDeviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users user;

    @Column(name = "installation_id", nullable = false, length = 100)
    private String installationId;

    @Column(nullable = false, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PushPlatform platform;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private PushDevice(
            Users user,
            String installationId,
            String token,
            PushPlatform platform
    ) {
        LocalDateTime now = LocalDateTime.now();
        this.user = user;
        this.installationId = installationId;
        this.token = token;
        this.platform = platform;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static PushDevice create(
            Users user,
            String installationId,
            String token,
            PushPlatform platform
    ) {
        return new PushDevice(user, installationId, token, platform);
    }

    public void updateRegistration(
            Users user,
            String installationId,
            String token,
            PushPlatform platform
    ) {
        this.user = user;
        this.installationId = installationId;
        this.token = token;
        this.platform = platform;
        this.updatedAt = LocalDateTime.now();
    }
}
