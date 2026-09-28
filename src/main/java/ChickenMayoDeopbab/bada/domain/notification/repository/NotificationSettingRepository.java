package ChickenMayoDeopbab.bada.domain.notification.repository;

import ChickenMayoDeopbab.bada.domain.notification.entity.NotificationSetting;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    void deleteByUser(Users user);
}
