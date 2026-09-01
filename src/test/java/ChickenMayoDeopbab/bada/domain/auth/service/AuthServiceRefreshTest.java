package ChickenMayoDeopbab.bada.domain.auth.service;

import ChickenMayoDeopbab.bada.domain.auth.dto.request.RefreshRequest;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import ChickenMayoDeopbab.bada.global.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceRefreshTest {

    private static final long USER_ID = 1L;
    private static final String STORED_KEY = "refreshToken: " + USER_ID;

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

    private final UsersRepository usersRepository = mock(UsersRepository.class);
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);

    private final AuthService authService =
            new AuthService(usersRepository, redisTemplate, jwtProvider, passwordEncoder);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("틀린 refreshToken으로는 저장된 토큰을 지우지 못한다")
    void doesNotEvictStoredTokenOnMismatch() {
        when(valueOperations.get(STORED_KEY)).thenReturn("진짜-리프레시-토큰");

        assertThatThrownBy(() -> authService.refresh(
                new RefreshRequest("아무-문자열", USER_ID), new MockHttpServletResponse()))
                .isInstanceOf(ApplicationException.class);

        // userId만 알면 누구나 남의 세션을 끊을 수 있으면 안 된다.
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("저장된 토큰이 없어도 삭제를 시도하지 않는다")
    void doesNotEvictWhenNothingStored() {
        when(valueOperations.get(STORED_KEY)).thenReturn(null);

        assertThatThrownBy(() -> authService.refresh(
                new RefreshRequest("아무-문자열", USER_ID), new MockHttpServletResponse()))
                .isInstanceOf(ApplicationException.class);

        verify(redisTemplate, never()).delete(anyString());
    }
}
