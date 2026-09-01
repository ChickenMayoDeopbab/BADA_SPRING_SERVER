package ChickenMayoDeopbab.bada.domain.notification.service;

import ChickenMayoDeopbab.bada.domain.notification.dto.request.UpdateNotificationSettingRequest;
import ChickenMayoDeopbab.bada.domain.notification.dto.response.NotificationSettingResponse;
import ChickenMayoDeopbab.bada.domain.notification.entity.NotificationSetting;
import ChickenMayoDeopbab.bada.domain.notification.repository.NotificationSettingRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final UsersRepository usersRepository;

    @Transactional(readOnly = true)
    public NotificationSettingResponse get() {
        Users user = getUserInfo();
        return notificationSettingRepository.findById(user.getUserId())
                .map(NotificationSettingResponse::from)
                .orElseGet(NotificationSettingResponse::enabledByDefault);
    }

    @Transactional
    public NotificationSettingResponse update(UpdateNotificationSettingRequest request) {
        Users user = getUserInfo();
        NotificationSetting setting = notificationSettingRepository.findById(user.getUserId())
                .orElseGet(() -> NotificationSetting.enabledByDefault(user));

        setting.update(
                request.allEnabled(),
                request.communityEnabled(),
                request.trainingEnabled()
        );
        return NotificationSettingResponse.from(notificationSettingRepository.save(setting));
    }

    private Users getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return usersRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));
    }
}
