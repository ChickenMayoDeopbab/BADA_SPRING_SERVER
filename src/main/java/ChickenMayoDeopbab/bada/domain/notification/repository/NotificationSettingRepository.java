package ChickenMayoDeopbab.bada.domain.notification.repository;

import ChickenMayoDeopbab.bada.domain.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
}
