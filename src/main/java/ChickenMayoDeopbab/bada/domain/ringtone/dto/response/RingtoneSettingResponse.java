package ChickenMayoDeopbab.bada.domain.ringtone.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record RingtoneSettingResponse(boolean defaultRingtone, Long fileId, String title, String url) {

    public static RingtoneSettingResponse defaultSetting() {
        return new RingtoneSettingResponse(true, null, null, null);
    }

    public static RingtoneSettingResponse selected(RingtoneResponse ringtone) {
        return new RingtoneSettingResponse(false, ringtone.fileId(), ringtone.title(), ringtone.url());
    }
}
