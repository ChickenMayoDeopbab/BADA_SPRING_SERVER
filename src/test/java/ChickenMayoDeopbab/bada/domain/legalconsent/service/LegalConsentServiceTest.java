package ChickenMayoDeopbab.bada.domain.legalconsent.service;

import ChickenMayoDeopbab.bada.domain.legalconsent.dto.request.AcceptLegalConsentRequest;
import ChickenMayoDeopbab.bada.domain.legalconsent.dto.response.LegalConsentStatusResponse;
import ChickenMayoDeopbab.bada.domain.legalconsent.port.BiosignalDataCleanupPort;
import ChickenMayoDeopbab.bada.domain.file.service.FileService;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LegalConsentServiceTest {

    private Users user;
    private LegalConsentService legalConsentService;
    private FileService fileService;
    private BiosignalDataCleanupPort biosignalDataCleanupPort;

    @BeforeEach
    void setUp() {
        UsersRepository usersRepository = mock(UsersRepository.class);
        user = Users.builder()
                .userId(7L)
                .username("legal-consent-user")
                .build();
        fileService = mock(FileService.class);
        biosignalDataCleanupPort = mock(BiosignalDataCleanupPort.class);

        when(usersRepository.findByUsername("legal-consent-user"))
                .thenReturn(Optional.of(user));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("legal-consent-user", null)
        );
        legalConsentService = new LegalConsentService(usersRepository, fileService, biosignalDataCleanupPort);
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
        assertThat(response.biosignalAgreed()).isFalse();
        assertThat(response.legalActionRequired()).isTrue();
    }

    @Test
    void acceptStoresRequiredConsentWithoutForcingSensitiveConsent() {
        LegalConsentStatusResponse response = legalConsentService.accept(
                new AcceptLegalConsentRequest(true, true, false, false, false)
        );

        assertThat(response.termsOfServiceAgreed()).isTrue();
        assertThat(response.termsOfServiceAgreedAt()).isNotNull();
        assertThat(response.privacyPolicyAcknowledged()).isTrue();
        assertThat(response.privacyPolicyAcknowledgedAt()).isNotNull();
        assertThat(response.sensitiveInformationAgreed()).isFalse();
        assertThat(response.sensitiveInformationAgreedAt()).isNull();
        assertThat(response.profileImageAgreed()).isFalse();
        assertThat(response.biosignalAgreed()).isFalse();
        assertThat(response.biosignalAgreedAt()).isNull();
        verify(fileService).deleteProfileFilesByUserId(7L);
        assertThat(response.legalActionRequired()).isFalse();
    }

    @Test
    void withdrawSensitiveConsentKeepsRequiredConsent() {
        legalConsentService.accept(new AcceptLegalConsentRequest(true, true, true, false, false));

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
                new AcceptLegalConsentRequest(true, true, false, true, false)
        );

        assertThat(accepted.profileImageAgreed()).isTrue();
        assertThat(accepted.profileImageAgreedAt()).isNotNull();

        LegalConsentStatusResponse withdrawn = legalConsentService.withdrawProfileImage();

        assertThat(withdrawn.profileImageAgreed()).isFalse();
        assertThat(withdrawn.profileImageWithdrawnAt()).isNotNull();
        verify(fileService).deleteProfileFilesByUserId(7L);
    }

    @Test
    void acceptStoresBiosignalConsentIndependentlyOfSensitiveConsent() {
        LegalConsentStatusResponse response = legalConsentService.accept(
                new AcceptLegalConsentRequest(true, true, false, false, true)
        );

        assertThat(response.biosignalAgreed()).isTrue();
        assertThat(response.biosignalAgreedAt()).isNotNull();
        assertThat(response.biosignalWithdrawnAt()).isNull();
        assertThat(response.sensitiveInformationAgreed()).isFalse();
    }

    @Test
    void acceptWithoutBiosignalChoiceKeepsExistingBiosignalConsent() {
        legalConsentService.accept(new AcceptLegalConsentRequest(true, true, false, false, true));
        Instant agreedAt = user.getBiosignalAgreedAt();

        LegalConsentStatusResponse omitted = legalConsentService.accept(
                new AcceptLegalConsentRequest(true, true, false, false, null)
        );
        LegalConsentStatusResponse declined = legalConsentService.accept(
                new AcceptLegalConsentRequest(true, true, false, false, false)
        );

        assertThat(omitted.biosignalAgreed()).isTrue();
        assertThat(declined.biosignalAgreed()).isTrue();
        assertThat(declined.biosignalAgreedAt()).isEqualTo(agreedAt);
    }

    @Test
    void withdrawBiosignalConsentKeepsOtherConsentsAndCanBeAgreedAgain() {
        legalConsentService.accept(new AcceptLegalConsentRequest(true, true, true, false, true));

        LegalConsentStatusResponse withdrawn = legalConsentService.withdrawBiosignal();

        assertThat(withdrawn.biosignalAgreed()).isFalse();
        assertThat(withdrawn.biosignalAgreedAt()).isNotNull();
        assertThat(withdrawn.biosignalWithdrawnAt()).isNotNull();
        assertThat(withdrawn.sensitiveInformationAgreed()).isTrue();
        assertThat(withdrawn.legalActionRequired()).isFalse();

        LegalConsentStatusResponse agreedAgain = legalConsentService.accept(
                new AcceptLegalConsentRequest(true, true, true, false, true)
        );

        assertThat(agreedAgain.biosignalAgreed()).isTrue();
        assertThat(agreedAgain.biosignalWithdrawnAt()).isNull();
    }

    @Test
    void withdrawBiosignalPurgesFastApiDataBeforeRecordingWithdrawal() {
        legalConsentService.accept(new AcceptLegalConsentRequest(true, true, false, false, true));
        doAnswer(invocation -> {
            assertThat(user.isBiosignalAgreed()).isTrue();
            return null;
        }).when(biosignalDataCleanupPort).deleteByUserId(7L);

        LegalConsentStatusResponse withdrawn = legalConsentService.withdrawBiosignal();

        verify(biosignalDataCleanupPort).deleteByUserId(7L);
        assertThat(withdrawn.biosignalAgreed()).isFalse();
        assertThat(withdrawn.biosignalWithdrawnAt()).isNotNull();
    }

    @Test
    void withdrawBiosignalWithoutConsentDoesNotCallFastApi() {
        LegalConsentStatusResponse response = legalConsentService.withdrawBiosignal();

        verifyNoInteractions(biosignalDataCleanupPort);
        assertThat(response.biosignalAgreed()).isFalse();
        assertThat(response.biosignalWithdrawnAt()).isNull();
    }

    @Test
    void withdrawBiosignalKeepsConsentWhenPurgeFails() {
        legalConsentService.accept(new AcceptLegalConsentRequest(true, true, false, false, true));
        doThrow(new IllegalStateException("fastapi down")).when(biosignalDataCleanupPort).deleteByUserId(7L);

        assertThatThrownBy(() -> legalConsentService.withdrawBiosignal())
                .isInstanceOf(IllegalStateException.class);

        assertThat(user.isBiosignalAgreed()).isTrue();
        assertThat(user.getBiosignalWithdrawnAt()).isNull();
    }
}
