package ChickenMayoDeopbab.bada.domain.ringtone.dto.request;

import jakarta.validation.constraints.Positive;

public record UpdateRingtoneSettingRequest(@Positive Long fileId) {
}
