package ChickenMayoDeopbab.bada.domain.user.service;

import ChickenMayoDeopbab.bada.domain.user.entity.UserStatus;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAccessPolicyTest {

    private final UserAccessPolicy policy = new UserAccessPolicy();

    @Test
    void allowsActiveUser() {
        assertThatCode(() -> policy.ensureCanAccess(user(UserStatus.ACTIVE, null))).doesNotThrowAnyException();
    }

    @Test
    void rejectsSuspendedUser() {
        assertRestricted(UserStatus.SUSPENDED, Instant.now().plusSeconds(3600), UsersStatusCode.USER_SUSPENDED);
    }

    @Test
    void rejectsBannedUser() {
        assertRestricted(UserStatus.BANNED, null, UsersStatusCode.USER_BANNED);
    }

    @Test
    void activatesExpiredSuspension() {
        Users user = user(UserStatus.SUSPENDED, Instant.now().minusSeconds(1));

        assertThatCode(() -> policy.ensureCanAccess(user)).doesNotThrowAnyException();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    private void assertRestricted(UserStatus status, Instant suspendedUntil, UsersStatusCode expected) {
        assertThatThrownBy(() -> policy.ensureCanAccess(user(status, suspendedUntil)))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getStatusCode())
                .isEqualTo(expected);
    }

    private Users user(UserStatus status, Instant suspendedUntil) {
        Users user = Users.builder().status(UserStatus.ACTIVE).build();
        if (status != UserStatus.ACTIVE) {
            user.updateModerationStatus(status, suspendedUntil, Instant.now(), 9L, "제재");
        }
        return user;
    }
}
