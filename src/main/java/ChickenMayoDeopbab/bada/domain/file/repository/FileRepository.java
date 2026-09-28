package ChickenMayoDeopbab.bada.domain.file.repository;

import ChickenMayoDeopbab.bada.domain.file.entity.File;
import ChickenMayoDeopbab.bada.domain.file.enumeration.FileType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {
    Optional<File> findByS3Key(String s3Key);

    List<File> findAllByUserId(Long userId);

    List<File> findAllByUserIdAndFileType(Long userId, FileType fileType);
}
