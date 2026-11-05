package ChickenMayoDeopbab.bada.domain.user.entity;

import jakarta.persistence.Column;
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

    @Test
    void biosignalConsentCanBeWithdrawnAndAcceptedAgain() {
        Users user = Users.builder().build();
        Instant firstAgreedAt = Instant.parse("2026-11-05T01:00:00Z");
        Instant withdrawnAt = Instant.parse("2026-11-05T02:00:00Z");
        Instant agreedAgainAt = Instant.parse("2026-11-05T03:00:00Z");

        assertThat(user.isBiosignalAgreed()).isFalse();

        user.agreeBiosignal(firstAgreedAt);

        assertThat(user.isBiosignalAgreed()).isTrue();
        assertThat(user.isSensitiveInformationAgreed()).isFalse();

        user.withdrawBiosignal(withdrawnAt);

        assertThat(user.isBiosignalAgreed()).isFalse();
        assertThat(user.getBiosignalAgreedAt()).isEqualTo(firstAgreedAt);
        assertThat(user.getBiosignalWithdrawnAt()).isEqualTo(withdrawnAt);

        user.agreeBiosignal(agreedAgainAt);

        assertThat(user.isBiosignalAgreed()).isTrue();
        assertThat(user.getBiosignalAgreedAt()).isEqualTo(agreedAgainAt);
        assertThat(user.getBiosignalWithdrawnAt()).isNull();
    }

    @Test
    void repeatedBiosignalConsentAndWithdrawalAreIdempotent() {
        Users user = Users.builder().build();
        Instant withdrawnBeforeAgreeAt = Instant.parse("2026-11-05T00:30:00Z");
        Instant firstAgreedAt = Instant.parse("2026-11-05T01:00:00Z");
        Instant repeatedAgreedAt = Instant.parse("2026-11-05T02:00:00Z");
        Instant firstWithdrawnAt = Instant.parse("2026-11-05T03:00:00Z");
        Instant repeatedWithdrawnAt = Instant.parse("2026-11-05T04:00:00Z");

        user.withdrawBiosignal(withdrawnBeforeAgreeAt);

        assertThat(user.getBiosignalWithdrawnAt()).isNull();

        user.agreeBiosignal(firstAgreedAt);
        user.agreeBiosignal(repeatedAgreedAt);
        user.withdrawBiosignal(firstWithdrawnAt);
        user.withdrawBiosignal(repeatedWithdrawnAt);

        assertThat(user.getBiosignalAgreedAt()).isEqualTo(firstAgreedAt);
        assertThat(user.getBiosignalWithdrawnAt()).isEqualTo(firstWithdrawnAt);
    }

    @Test
    void biosignalConsentColumnsKeepNamesFastApiReads() throws NoSuchFieldException {
        assertThat(Users.class.getDeclaredField("biosignalAgreedAt").getAnnotation(Column.class).name())
                .isEqualTo("biosignal_agreed_at");
        assertThat(Users.class.getDeclaredField("biosignalWithdrawnAt").getAnnotation(Column.class).name())
                .isEqualTo("biosignal_withdrawn_at");
    }
}
