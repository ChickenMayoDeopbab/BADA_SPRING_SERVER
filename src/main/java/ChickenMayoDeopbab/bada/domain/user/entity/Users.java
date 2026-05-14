package ChickenMayoDeopbab.bada.domain.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    private String password;

    @Column(nullable = false)
    private String email;

    private String profileImage = "https://dasjkdj.s3.ap-northeast-2.amazonaws.com/default_profile.png";

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

    public Users update(String profileImage) {
        this.profileImage = profileImage;
        return this;
    }
}
