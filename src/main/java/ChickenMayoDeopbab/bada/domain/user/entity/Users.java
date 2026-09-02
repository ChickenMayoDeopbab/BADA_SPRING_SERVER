package ChickenMayoDeopbab.bada.domain.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "providerId"})
})
@Builder
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(length = 50, unique = true, nullable = false)
    private String username;

    @Setter
    private String password;

    @Column(length = 50)
    private String name;

    // 소셜 로그인은 이메일이 없을 수 있다(네이버 선택 동의 거부, 애플 이메일 가리기 등).
    private String email;

    private String profileImage;

    @Column(columnDefinition = "integer default 0")
    private int totalTrainingCount = 0;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    private String providerId;

    @Builder.Default
    @Column(nullable = false)
    private boolean paymentIntended = false;


    @PrePersist
    public void prePersist() {
        this.role = Role.USER;
        this.totalTrainingCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (provider == null) {
            this.provider = Provider.LOCAL;
            this.providerId = UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean applyProfileImageIfAbsent(String profileImage) {
        if (profileImage == null || profileImage.isBlank()) {
            return false;
        }
        if (this.profileImage != null && !this.profileImage.isBlank()) {
            return false;
        }
        this.profileImage = profileImage;
        return true;
    }

    public void update(String name, String username, String s3Key) {
        this.name = name;
        this.username = username;
        this.profileImage = s3Key;
    }

    public void intendPayment() {
        this.paymentIntended = true;
    }
}
