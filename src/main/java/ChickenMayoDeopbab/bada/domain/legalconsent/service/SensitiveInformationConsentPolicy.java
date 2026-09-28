package ChickenMayoDeopbab.bada.domain.legalconsent.service;

import ChickenMayoDeopbab.bada.domain.legalconsent.exception.LegalConsentStatusCode;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.springframework.stereotype.Component;

@Component
public class SensitiveInformationConsentPolicy {

    public void ensureAgreed(Users user) {
        if (user == null || !user.isSensitiveInformationAgreed()) {
            throw new ApplicationException(
                    LegalConsentStatusCode.SENSITIVE_INFORMATION_CONSENT_REQUIRED
            );
        }
    }
}
