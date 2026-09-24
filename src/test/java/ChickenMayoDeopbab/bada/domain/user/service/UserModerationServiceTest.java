package ChickenMayoDeopbab.bada.domain.user.service;

import ChickenMayoDeopbab.bada.domain.user.dto.request.UserModerationStatusRequest;
import ChickenMayoDeopbab.bada.domain.user.entity.Role;
import ChickenMayoDeopbab.bada.domain.user.entity.UserStatus;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserModerationServiceTest {

    private final UsersRepository usersRepository = mock(UsersRepository.class);
    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    private final UserModerationService service = new UserModerationService(usersRepository, redisTemplate);

    @Test
    void suspendsUserAndRevokesRefreshToken() {
        Users target = user(7L, Role.USER);
        Instant suspendedUntil = Instant.now().plus(1, ChronoUnit.DAYS);
        givenUsers(target);

        service.updateStatus(7L, new UserModerationStatusRequest(
                UserStatus.SUSPENDED, suspendedUntil, " 반복적인 괴롭힘 ", 9L));

        assertThat(target.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(target.getSuspendedUntil()).isEqualTo(suspendedUntil);
        assertThat(target.getSanctionedByUserId()).isEqualTo(9L);
        assertThat(target.getSanctionReason()).isEqualTo("반복적인 괴롭힘");
        verify(redisTemplate).delete("refreshToken: 7");
    }

    @Test
    void bansUserWithoutSuspensionDeadline() {
        Users target = user(7L, Role.USER);
        givenUsers(target);

        service.updateStatus(7L, new UserModerationStatusRequest(UserStatus.BANNED, null, "심각한 위반", 9L));

        assertThat(target.getStatus()).isEqualTo(UserStatus.BANNED);
        assertThat(target.getSuspendedUntil()).isNull();
        verify(redisTemplate).delete("refreshToken: 7");
    }

    @Test
    void activatesUserWithoutCreatingSession() {
        Users target = user(7L, Role.USER);
        target.updateModerationStatus(UserStatus.BANNED, null, Instant.now(), 9L, "위반");
        givenUsers(target);

        service.updateStatus(7L, new UserModerationStatusRequest(UserStatus.ACTIVE, null, null, 9L));

        assertThat(target.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(redisTemplate, never()).delete("refreshToken: 7");
    }

    @Test
    void rejectsExpiredSuspensionDeadline() {
        Users target = user(7L, Role.USER);
        givenUsers(target);

        assertThatThrownBy(() -> service.updateStatus(7L, new UserModerationStatusRequest(
                UserStatus.SUSPENDED, Instant.now().minusSeconds(1), "제재", 9L)))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getStatusCode())
                .isEqualTo(UsersStatusCode.INVALID_MODERATION_STATUS);

        assertThat(target.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(redisTemplate, never()).delete("refreshToken: 7");
    }

    @Test
    void rejectsNonAdminActor() {
        Users actor = user(8L, Role.USER);
        when(usersRepository.findById(8L)).thenReturn(Optional.of(actor));

        assertThatThrownBy(() -> service.updateStatus(7L, new UserModerationStatusRequest(
                UserStatus.BANNED, null, "제재", 8L)))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getStatusCode())
                .isEqualTo(UsersStatusCode.MODERATOR_NOT_ALLOWED);

        verify(usersRepository, never()).findById(7L);
    }

    @Test
    void rejectsAdminTarget() {
        Users target = user(7L, Role.ADMIN);
        givenUsers(target);

        assertThatThrownBy(() -> service.updateStatus(7L, new UserModerationStatusRequest(
                UserStatus.BANNED, null, "제재", 9L)))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getStatusCode())
                .isEqualTo(UsersStatusCode.CANNOT_SANCTION_ADMIN);

        verify(redisTemplate, never()).delete("refreshToken: 7");
    }

    private void givenUsers(Users target) {
        when(usersRepository.findById(9L)).thenReturn(Optional.of(user(9L, Role.ADMIN)));
        when(usersRepository.findById(target.getUserId())).thenReturn(Optional.of(target));
    }

    private Users user(Long userId, Role role) {
        return Users.builder().userId(userId).role(role).status(UserStatus.ACTIVE).build();
    }
}
