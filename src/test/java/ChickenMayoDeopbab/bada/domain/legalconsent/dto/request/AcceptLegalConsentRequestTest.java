package ChickenMayoDeopbab.bada.domain.legalconsent.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcceptLegalConsentRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requiredLegalConsentMustBeTrue() {
        AcceptLegalConsentRequest request = new AcceptLegalConsentRequest(false, false, true);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("termsOfServiceAgreed", "privacyPolicyAcknowledged");
    }

    @Test
    void sensitiveConsentChoiceMustBeExplicit() {
        AcceptLegalConsentRequest request = new AcceptLegalConsentRequest(true, true, null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("sensitiveInformationAgreed");
    }

    @Test
    void sensitiveConsentMayBeDeclined() {
        AcceptLegalConsentRequest request = new AcceptLegalConsentRequest(true, true, false);

        assertThat(validator.validate(request)).isEmpty();
    }
}
