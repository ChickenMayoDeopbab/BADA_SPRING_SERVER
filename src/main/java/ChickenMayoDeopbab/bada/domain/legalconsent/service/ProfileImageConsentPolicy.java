package ChickenMayoDeopbab.bada.domain.legalconsent.service;

import ChickenMayoDeopbab.bada.domain.legalconsent.exception.LegalConsentStatusCode;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.springframework.stereotype.Component;

@Component
public class ProfileImageConsentPolicy {

    public void ensureAgreed(Users user) {
        if (user == null || !user.isProfileImageAgreed()) {
            throw new ApplicationException(
                    LegalConsentStatusCode.PROFILE_IMAGE_CONSENT_REQUIRED
            );
        }
    }
}
