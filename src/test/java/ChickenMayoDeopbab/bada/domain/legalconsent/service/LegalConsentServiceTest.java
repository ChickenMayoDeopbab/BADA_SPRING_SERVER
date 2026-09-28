package ChickenMayoDeopbab.bada.domain.legalconsent.service;

import ChickenMayoDeopbab.bada.domain.legalconsent.dto.request.AcceptLegalConsentRequest;
import ChickenMayoDeopbab.bada.domain.legalconsent.dto.response.LegalConsentStatusResponse;
import ChickenMayoDeopbab.bada.domain.file.service.FileService;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegalConsentServiceTest {

    private Users user;
    private LegalConsentService legalConsentService;
    private FileService fileService;

    @BeforeEach
    void setUp() {
        UsersRepository usersRepository = mock(UsersRepository.class);
        user = Users.builder()
                .userId(7L)
                .username("legal-consent-user")
                .build();
        fileService = mock(FileService.class);

        when(usersRepository.findByUsername("legal-consent-user"))
                .thenReturn(Optional.of(user));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("legal-consent-user", null)
        );
        legalConsentService = new LegalConsentService(usersRepository, fileService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getStatusRequiresActionBeforeRequiredConsent() {
        LegalConsentStatusResponse response = legalConsentService.getStatus();

        assertThat(response.termsOfServiceAgreed()).isFalse();
        assertThat(response.privacyPolicyAcknowledged()).isFalse();
        assertThat(response.sensitiveInformationAgreed()).isFalse();
        assertThat(response.profileImageAgreed()).isFalse();
        assertThat(response.legalActionRequired()).isTrue();
    }

    @Test
    void acceptStoresRequiredConsentWithoutForcingSensitiveConsent() {
        LegalConsentStatusResponse response = legalConsentService.accept(
                new AcceptLegalConsentRequest(true, true, false, false)
        );

        assertThat(response.termsOfServiceAgreed()).isTrue();
        assertThat(response.termsOfServiceAgreedAt()).isNotNull();
        assertThat(response.privacyPolicyAcknowledged()).isTrue();
        assertThat(response.privacyPolicyAcknowledgedAt()).isNotNull();
        assertThat(response.sensitiveInformationAgreed()).isFalse();
        assertThat(response.sensitiveInformationAgreedAt()).isNull();
        assertThat(response.profileImageAgreed()).isFalse();
        verify(fileService).deleteProfileFilesByUserId(7L);
        assertThat(response.legalActionRequired()).isFalse();
    }

    @Test
    void withdrawSensitiveConsentKeepsRequiredConsent() {
        legalConsentService.accept(new AcceptLegalConsentRequest(true, true, true, false));

        LegalConsentStatusResponse response = legalConsentService.withdrawSensitiveInformation();

        assertThat(response.termsOfServiceAgreed()).isTrue();
        assertThat(response.privacyPolicyAcknowledged()).isTrue();
        assertThat(response.sensitiveInformationAgreed()).isFalse();
        assertThat(response.sensitiveInformationAgreedAt()).isNotNull();
        assertThat(response.sensitiveInformationWithdrawnAt()).isNotNull();
        assertThat(response.legalActionRequired()).isFalse();
    }

    @Test
    void profileImageConsentCanBeAcceptedAndWithdrawnWithDataDeletion() {
        LegalConsentStatusResponse accepted = legalConsentService.accept(
                new AcceptLegalConsentRequest(true, true, false, true)
        );

        assertThat(accepted.profileImageAgreed()).isTrue();
        assertThat(accepted.profileImageAgreedAt()).isNotNull();

        LegalConsentStatusResponse withdrawn = legalConsentService.withdrawProfileImage();

        assertThat(withdrawn.profileImageAgreed()).isFalse();
        assertThat(withdrawn.profileImageWithdrawnAt()).isNotNull();
        verify(fileService).deleteProfileFilesByUserId(7L);
    }
}
