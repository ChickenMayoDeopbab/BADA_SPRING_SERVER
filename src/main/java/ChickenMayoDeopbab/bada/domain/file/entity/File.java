package ChickenMayoDeopbab.bada.domain.file.entity;

import ChickenMayoDeopbab.bada.domain.file.enumeration.FileType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type")
    private FileType fileType;

    @Column(name = "s3_key")
    private String s3Key;

    @Builder
    private File(String title, FileType fileType, String s3Key) {
        this.title = title;
        this.fileType = fileType;
        this.s3Key = s3Key;
    }
}
