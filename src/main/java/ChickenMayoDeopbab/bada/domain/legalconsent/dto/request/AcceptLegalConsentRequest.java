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
        Boolean sensitiveInformationAgreed
) {
}
