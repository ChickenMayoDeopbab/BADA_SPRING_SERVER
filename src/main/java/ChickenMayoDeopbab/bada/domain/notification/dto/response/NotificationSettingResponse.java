package ChickenMayoDeopbab.bada.domain.notification.dto.response;

import ChickenMayoDeopbab.bada.domain.notification.entity.NotificationSetting;

public record NotificationSettingResponse(
        boolean allEnabled,
        boolean communityEnabled,
        boolean trainingEnabled
) {
    public static NotificationSettingResponse enabledByDefault() {
        return new NotificationSettingResponse(true, true, true);
    }

    public static NotificationSettingResponse from(NotificationSetting setting) {
        return new NotificationSettingResponse(
                setting.isAllEnabled(),
                setting.isCommunityEnabled(),
                setting.isTrainingEnabled()
        );
    }
}
