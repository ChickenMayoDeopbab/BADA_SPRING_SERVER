package ChickenMayoDeopbab.bada.domain.legalconsent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record AcceptLegalConsentRequest(
        @Schema(description = "서비스 이용약관 필수 동의 여부", example = "true")
        @AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
        boolean termsOfServiceAgreed,

        @Schema(description = "개인정보처리방침 필수 확인 여부", example = "true")
        @AssertTrue(message = "개인정보처리방침을 확인해야 합니다.")
        boolean privacyPolicyAcknowledged,

        @Schema(description = "민감정보 처리 선택 동의 여부", example = "true")
        @NotNull(message = "민감정보 처리 동의 여부를 선택해야 합니다.")
        Boolean sensitiveInformationAgreed,

        @Schema(description = "프로필 이미지 처리 선택 동의 여부", example = "false")
        @NotNull(message = "프로필 이미지 처리 동의 여부를 선택해야 합니다.")
        Boolean profileImageAgreed,

        @Schema(description = "생체신호 처리 선택 동의 여부. true 면 동의를 저장한다. "
                + "false 를 보내거나 생략하면 기존 상태를 그대로 둔다(false 로 철회되지 않음). "
                + "철회는 DELETE /api/v1/legal-consents/biosignal 로만 한다.",
                example = "true")
        Boolean biosignalAgreed
) {
}
