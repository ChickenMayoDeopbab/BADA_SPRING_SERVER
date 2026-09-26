package ChickenMayoDeopbab.bada.domain.auth.service;

import ChickenMayoDeopbab.bada.domain.auth.dto.request.LoginRequest;
import ChickenMayoDeopbab.bada.domain.auth.dto.request.RefreshRequest;
import ChickenMayoDeopbab.bada.domain.user.entity.Role;
import ChickenMayoDeopbab.bada.domain.user.entity.UserStatus;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.domain.user.service.UserAccessPolicy;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import ChickenMayoDeopbab.bada.global.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceSanctionTest {

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final UsersRepository usersRepository = mock(UsersRepository.class);
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
    private final AuthService service = new AuthService(
            usersRepository, redisTemplate, jwtProvider, passwordEncoder, new UserAccessPolicy());

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void rejectsBannedUserLoginAfterPasswordVerification() {
        Users user = restrictedUser(UserStatus.BANNED, null);
        when(usersRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);

        assertStatus(() -> service.login(
                new LoginRequest("user", "password"), new MockHttpServletResponse()), UsersStatusCode.USER_BANNED);

        verify(jwtProvider, never()).createAccessToken(any(), any());
        verify(passwordEncoder).matches("password", "encoded");
    }

    @Test
    void rejectsSuspendedUserRefreshAfterTokenVerification() {
        Users user = restrictedUser(UserStatus.SUSPENDED, Instant.now().plusSeconds(3600));
        when(valueOperations.get("refreshToken: 7")).thenReturn("stored-token");
        when(usersRepository.findById(7L)).thenReturn(Optional.of(user));

        assertStatus(() -> service.refresh(
                new RefreshRequest("stored-token", 7L), new MockHttpServletResponse()),
                UsersStatusCode.USER_SUSPENDED);

        verify(jwtProvider, never()).createAccessToken(any(), any());
    }

    @Test
    void rejectsBannedUserBeforeIssuingOAuthCode() {
        when(usersRepository.findById(7L)).thenReturn(Optional.of(restrictedUser(UserStatus.BANNED, null)));

        assertStatus(() -> service.issueOAuthCode(7L), UsersStatusCode.USER_BANNED);

        verify(valueOperations, never()).set(any(), any(), any());
    }

    private Users restrictedUser(UserStatus status, Instant suspendedUntil) {
        Users user = Users.builder()
                .userId(7L)
                .username("user")
                .password("encoded")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        user.updateModerationStatus(status, suspendedUntil, Instant.now(), 9L, "제재");
        return user;
    }

    private void assertStatus(Runnable action, UsersStatusCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getStatusCode())
                .isEqualTo(expected);
    }
}
