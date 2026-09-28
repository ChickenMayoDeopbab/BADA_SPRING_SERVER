package ChickenMayoDeopbab.bada.domain.user.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UsersLegalConsentTest {

    @Test
    void repeatedRequiredConsentKeepsFirstAcceptedAt() {
        Users user = Users.builder().build();
        Instant firstAcceptedAt = Instant.parse("2026-09-28T01:00:00Z");
        Instant repeatedAcceptedAt = Instant.parse("2026-09-28T02:00:00Z");

        user.acceptRequiredLegalDocuments(firstAcceptedAt);
        user.acceptRequiredLegalDocuments(repeatedAcceptedAt);

        assertThat(user.getTermsAgreedAt()).isEqualTo(firstAcceptedAt);
        assertThat(user.getPrivacyPolicyAcknowledgedAt()).isEqualTo(firstAcceptedAt);
    }

    @Test
    void sensitiveConsentCanBeWithdrawnAndAcceptedAgain() {
        Users user = Users.builder().build();
        Instant firstAgreedAt = Instant.parse("2026-09-28T01:00:00Z");
        Instant withdrawnAt = Instant.parse("2026-09-28T02:00:00Z");
        Instant agreedAgainAt = Instant.parse("2026-09-28T03:00:00Z");

        user.agreeSensitiveInformation(firstAgreedAt);
        user.withdrawSensitiveInformation(withdrawnAt);

        assertThat(user.isSensitiveInformationAgreed()).isFalse();
        assertThat(user.getSensitiveInformationAgreedAt()).isEqualTo(firstAgreedAt);
        assertThat(user.getSensitiveInformationWithdrawnAt()).isEqualTo(withdrawnAt);

        user.agreeSensitiveInformation(agreedAgainAt);

        assertThat(user.isSensitiveInformationAgreed()).isTrue();
        assertThat(user.getSensitiveInformationAgreedAt()).isEqualTo(agreedAgainAt);
        assertThat(user.getSensitiveInformationWithdrawnAt()).isNull();
    }

    @Test
    void repeatedSensitiveConsentAndWithdrawalAreIdempotent() {
        Users user = Users.builder().build();
        Instant firstAgreedAt = Instant.parse("2026-09-28T01:00:00Z");
        Instant repeatedAgreedAt = Instant.parse("2026-09-28T02:00:00Z");
        Instant firstWithdrawnAt = Instant.parse("2026-09-28T03:00:00Z");
        Instant repeatedWithdrawnAt = Instant.parse("2026-09-28T04:00:00Z");

        user.agreeSensitiveInformation(firstAgreedAt);
        user.agreeSensitiveInformation(repeatedAgreedAt);
        user.withdrawSensitiveInformation(firstWithdrawnAt);
        user.withdrawSensitiveInformation(repeatedWithdrawnAt);

        assertThat(user.getSensitiveInformationAgreedAt()).isEqualTo(firstAgreedAt);
        assertThat(user.getSensitiveInformationWithdrawnAt()).isEqualTo(firstWithdrawnAt);
    }
}
