package ChickenMayoDeopbab.bada.domain.user.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UsersModerationStatusTest {

    @Test
    void newUserDefaultsToActive() {
        Users user = Users.builder().build();

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void suspensionStoresModerationMetadata() {
        Users user = Users.builder().build();
        Instant now = Instant.parse("2026-09-24T00:00:00Z");
        Instant until = now.plusSeconds(3600);

        user.updateModerationStatus(UserStatus.SUSPENDED, until, now, 9L, "반복적인 괴롭힘");

        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(user.getSuspendedUntil()).isEqualTo(until);
        assertThat(user.getSanctionedAt()).isEqualTo(now);
        assertThat(user.getSanctionedByUserId()).isEqualTo(9L);
        assertThat(user.getSanctionReason()).isEqualTo("반복적인 괴롭힘");
    }

    @Test
    void expiredSuspensionReturnsUserToActive() {
        Users user = Users.builder().build();
        Instant now = Instant.parse("2026-09-24T00:00:00Z");
        user.updateModerationStatus(UserStatus.SUSPENDED, now.minusSeconds(1), now.minusSeconds(3600), 9L, "제재");

        boolean changed = user.activateIfSuspensionExpired(now);

        assertThat(changed).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getSuspendedUntil()).isNull();
        assertThat(user.getSanctionedAt()).isNull();
        assertThat(user.getSanctionedByUserId()).isNull();
        assertThat(user.getSanctionReason()).isNull();
    }

    @Test
    void activeSuspensionIsNotClearedEarly() {
        Users user = Users.builder().build();
        Instant now = Instant.parse("2026-09-24T00:00:00Z");
        user.updateModerationStatus(UserStatus.SUSPENDED, now.plusSeconds(1), now, 9L, "제재");

        assertThat(user.activateIfSuspensionExpired(now)).isFalse();
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }
}
