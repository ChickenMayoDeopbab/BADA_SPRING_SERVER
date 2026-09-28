package ChickenMayoDeopbab.bada.domain.legalconsent.service;

import ChickenMayoDeopbab.bada.domain.legalconsent.exception.LegalConsentStatusCode;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileImageConsentPolicyTest {

    private final ProfileImageConsentPolicy policy = new ProfileImageConsentPolicy();

    @Test
    void allowsUserWithActiveProfileImageConsent() {
        Users user = Users.builder().build();
        user.agreeProfileImage(Instant.parse("2026-09-28T01:00:00Z"));

        assertThatCode(() -> policy.ensureAgreed(user)).doesNotThrowAnyException();
    }

    @Test
    void rejectsUserWithoutOrAfterProfileImageConsent() {
        Users user = Users.builder().build();

        assertThatThrownBy(() -> policy.ensureAgreed(user))
                .isInstanceOf(ApplicationException.class)
                .extracting("statusCode")
                .isEqualTo(LegalConsentStatusCode.PROFILE_IMAGE_CONSENT_REQUIRED);

        user.agreeProfileImage(Instant.parse("2026-09-28T01:00:00Z"));
        user.withdrawProfileImage(Instant.parse("2026-09-28T02:00:00Z"));

        assertThatThrownBy(() -> policy.ensureAgreed(user))
                .isInstanceOf(ApplicationException.class)
                .extracting("statusCode")
                .isEqualTo(LegalConsentStatusCode.PROFILE_IMAGE_CONSENT_REQUIRED);
    }
}
