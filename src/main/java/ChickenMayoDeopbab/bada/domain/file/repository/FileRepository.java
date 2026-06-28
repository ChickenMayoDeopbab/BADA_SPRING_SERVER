package ChickenMayoDeopbab.bada.domain.file.repository;

import ChickenMayoDeopbab.bada.domain.file.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {
}