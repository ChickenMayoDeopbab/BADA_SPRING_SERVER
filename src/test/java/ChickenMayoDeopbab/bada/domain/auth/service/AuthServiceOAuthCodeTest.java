package ChickenMayoDeopbab.bada.domain.auth.service;

import ChickenMayoDeopbab.bada.domain.auth.dto.response.OAuthTokenResponse;
import ChickenMayoDeopbab.bada.domain.user.entity.Role;
import ChickenMayoDeopbab.bada.domain.user.entity.UserStatus;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.domain.user.service.UserAccessPolicy;
import ChickenMayoDeopbab.bada.global.jwt.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 소셜 로그인 교환 코드에 신규 가입 여부를 실어 두었다가 토큰 교환 응답으로 돌려주는지 확인한다.
 */
class AuthServiceOAuthCodeTest {

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final UsersRepository usersRepository = mock(UsersRepository.class);
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final AuthService service = new AuthService(
            usersRepository, redisTemplate, jwtProvider, mock(BCryptPasswordEncoder.class), new UserAccessPolicy());

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(usersRepository.findById(7L)).thenReturn(Optional.of(Users.builder()
                .userId(7L).role(Role.USER).status(UserStatus.ACTIVE).build()));
        when(jwtProvider.createAccessToken(7L, Role.USER)).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(7L)).thenReturn("refresh-token");
    }

    @Test
    @DisplayName("교환 코드를 발급할 때 신규 가입 여부를 함께 저장한다")
    void storesNewUserFlagWithCode() {
        String code = service.issueOAuthCode(7L, true);

        verify(valueOperations).set(eq("oauthCode: " + code), eq("7:true"), any(Duration.class));
    }

    @Test
    @DisplayName("신규 가입자의 코드를 교환하면 isNewUser가 true다")
    void returnsNewUserOnExchange() {
        when(valueOperations.getAndDelete("oauthCode: code")).thenReturn("7:true");

        OAuthTokenResponse response = service.exchangeOAuthCode("code", new MockHttpServletResponse());

        assertThat(response.isNewUser()).isTrue();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("기존 회원의 코드를 교환하면 isNewUser가 false다")
    void returnsExistingUserOnExchange() {
        when(valueOperations.getAndDelete("oauthCode: code")).thenReturn("7:false");

        OAuthTokenResponse response = service.exchangeOAuthCode("code", new MockHttpServletResponse());

        assertThat(response.isNewUser()).isFalse();
    }

    @Test
    @DisplayName("배포 전에 userId만 저장된 코드도 기존 회원으로 교환된다")
    void exchangesLegacyCodeWithoutFlag() {
        when(valueOperations.getAndDelete("oauthCode: code")).thenReturn("7");

        OAuthTokenResponse response = service.exchangeOAuthCode("code", new MockHttpServletResponse());

        assertThat(response.isNewUser()).isFalse();
        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    @DisplayName("응답 JSON 필드명은 newUser가 아니라 isNewUser다")
    void serializesIsNewUserFieldName() throws Exception {
        String json = new ObjectMapper().writeValueAsString(
                new OAuthTokenResponse("access-token", "refresh-token", true));

        assertThat(json).contains("\"isNewUser\":true").doesNotContain("\"newUser\"");
    }
}
