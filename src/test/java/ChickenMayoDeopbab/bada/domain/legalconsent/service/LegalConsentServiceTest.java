package ChickenMayoDeopbab.bada.domain.legalconsent.service;

import ChickenMayoDeopbab.bada.domain.legalconsent.dto.request.AcceptLegalConsentRequest;
import ChickenMayoDeopbab.bada.domain.legalconsent.dto.response.LegalConsentStatusResponse;
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
import static org.mockito.Mockito.when;

class LegalConsentServiceTest {

    private Users user;
    private LegalConsentService legalConsentService;

    @BeforeEach
    void setUp() {
        UsersRepository usersRepository = mock(UsersRepository.class);
        user = Users.builder()
                .username("legal-consent-user")
                .build();

        when(usersRepository.findByUsername("legal-consent-user"))
                .thenReturn(Optional.of(user));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("legal-consent-user", null)
        );
        legalConsentService = new LegalConsentService(usersRepository);
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
        assertThat(response.legalActionRequired()).isTrue();
    }

    @Test
    void acceptStoresRequiredConsentWithoutForcingSensitiveConsent() {
        LegalConsentStatusResponse response = legalConsentService.accept(
                new AcceptLegalConsentRequest(true, true, false)
        );

        assertThat(response.termsOfServiceAgreed()).isTrue();
        assertThat(response.termsOfServiceAgreedAt()).isNotNull();
        assertThat(response.privacyPolicyAcknowledged()).isTrue();
        assertThat(response.privacyPolicyAcknowledgedAt()).isNotNull();
        assertThat(response.sensitiveInformationAgreed()).isFalse();
        assertThat(response.sensitiveInformationAgreedAt()).isNull();
        assertThat(response.legalActionRequired()).isFalse();
    }

    @Test
    void withdrawSensitiveConsentKeepsRequiredConsent() {
        legalConsentService.accept(new AcceptLegalConsentRequest(true, true, true));

        LegalConsentStatusResponse response = legalConsentService.withdrawSensitiveInformation();

        assertThat(response.termsOfServiceAgreed()).isTrue();
        assertThat(response.privacyPolicyAcknowledged()).isTrue();
        assertThat(response.sensitiveInformationAgreed()).isFalse();
        assertThat(response.sensitiveInformationAgreedAt()).isNotNull();
        assertThat(response.sensitiveInformationWithdrawnAt()).isNotNull();
        assertThat(response.legalActionRequired()).isFalse();
    }
}
