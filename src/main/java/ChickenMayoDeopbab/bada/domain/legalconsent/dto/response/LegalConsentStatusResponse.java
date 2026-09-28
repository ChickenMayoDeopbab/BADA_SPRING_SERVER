package ChickenMayoDeopbab.bada.domain.legalconsent.dto.response;

import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record LegalConsentStatusResponse(
        @Schema(description = "서비스 이용약관 동의 여부")
        boolean termsOfServiceAgreed,

        @Schema(description = "서비스 이용약관 최초 동의 시각")
        Instant termsOfServiceAgreedAt,

        @Schema(description = "개인정보처리방침 확인 여부")
        boolean privacyPolicyAcknowledged,

        @Schema(description = "개인정보처리방침 최초 확인 시각")
        Instant privacyPolicyAcknowledgedAt,

        @Schema(description = "현재 민감정보 처리 동의 여부")
        boolean sensitiveInformationAgreed,

        @Schema(description = "민감정보 처리 최근 동의 시각")
        Instant sensitiveInformationAgreedAt,

        @Schema(description = "민감정보 처리 최근 철회 시각")
        Instant sensitiveInformationWithdrawnAt,

        @Schema(description = "현재 프로필 이미지 처리 동의 여부")
        boolean profileImageAgreed,

        @Schema(description = "프로필 이미지 처리 최근 동의 시각")
        Instant profileImageAgreedAt,

        @Schema(description = "프로필 이미지 처리 최근 철회 시각")
        Instant profileImageWithdrawnAt,

        @Schema(description = "필수 약관 화면 표시 필요 여부")
        boolean legalActionRequired
) {
    public static LegalConsentStatusResponse from(Users user) {
        boolean termsOfServiceAgreed = user.getTermsAgreedAt() != null;
        boolean privacyPolicyAcknowledged = user.getPrivacyPolicyAcknowledgedAt() != null;

        return new LegalConsentStatusResponse(
                termsOfServiceAgreed,
                user.getTermsAgreedAt(),
                privacyPolicyAcknowledged,
                user.getPrivacyPolicyAcknowledgedAt(),
                user.isSensitiveInformationAgreed(),
                user.getSensitiveInformationAgreedAt(),
                user.getSensitiveInformationWithdrawnAt(),
                user.isProfileImageAgreed(),
                user.getProfileImageAgreedAt(),
                user.getProfileImageWithdrawnAt(),
                !termsOfServiceAgreed || !privacyPolicyAcknowledged
        );
    }
}
