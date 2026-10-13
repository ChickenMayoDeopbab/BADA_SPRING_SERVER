package ChickenMayoDeopbab.bada.domain.ringtone.entity;

import ChickenMayoDeopbab.bada.domain.file.entity.File;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "ringtone_settings")
public class RingtoneSetting {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_file_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private File selectedFile;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private RingtoneSetting(Users user) {
        this.user = user;
        this.updatedAt = LocalDateTime.now();
    }

    public static RingtoneSetting defaultFor(Users user) {
        return new RingtoneSetting(user);
    }

    public void select(File file) {
        this.selectedFile = file;
        this.updatedAt = LocalDateTime.now();
    }
}
