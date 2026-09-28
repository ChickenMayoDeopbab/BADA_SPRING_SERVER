package ChickenMayoDeopbab.bada.domain.legalconsent.service;

import ChickenMayoDeopbab.bada.domain.legalconsent.dto.request.AcceptLegalConsentRequest;
import ChickenMayoDeopbab.bada.domain.legalconsent.dto.response.LegalConsentStatusResponse;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LegalConsentService {

    private final UsersRepository usersRepository;

    public LegalConsentStatusResponse getStatus() {
        return LegalConsentStatusResponse.from(getCurrentUser());
    }

    @Transactional
    public LegalConsentStatusResponse accept(AcceptLegalConsentRequest request) {
        Users user = getCurrentUser();
        Instant acceptedAt = Instant.now();

        user.acceptRequiredLegalDocuments(acceptedAt);
        if (Boolean.TRUE.equals(request.sensitiveInformationAgreed())) {
            user.agreeSensitiveInformation(acceptedAt);
        }

        return LegalConsentStatusResponse.from(user);
    }

    @Transactional
    public LegalConsentStatusResponse withdrawSensitiveInformation() {
        Users user = getCurrentUser();
        user.withdrawSensitiveInformation(Instant.now());
        return LegalConsentStatusResponse.from(user);
    }

    private Users getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return usersRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));
    }
}
