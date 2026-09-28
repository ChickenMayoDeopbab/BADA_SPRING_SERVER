package ChickenMayoDeopbab.bada.domain.auth.handler;

import ChickenMayoDeopbab.bada.domain.auth.principal.SocialOAuth2User;
import ChickenMayoDeopbab.bada.domain.auth.principal.SocialOidcUser;
import ChickenMayoDeopbab.bada.domain.auth.service.AuthService;
import ChickenMayoDeopbab.bada.domain.auth.service.OAuthRedirectUriResolver;
import ChickenMayoDeopbab.bada.domain.user.entity.Provider;
import ChickenMayoDeopbab.bada.domain.user.entity.Role;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserService가 principal에 실어 보낸 신규 가입 여부가 교환 코드 발급까지 전달되는지 확인한다.
 */
class OAuth2LoginSuccessHandlerTest {

    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final AuthService authService = mock(AuthService.class);
    private final UsersRepository usersRepository = mock(UsersRepository.class);
    private final OAuthRedirectUriResolver redirectUriResolver = mock(OAuthRedirectUriResolver.class);
    private final OAuth2LoginSuccessHandler handler =
            new OAuth2LoginSuccessHandler(authService, usersRepository, redirectUriResolver);

    @BeforeEach
    void setUp() {
        when(redirectUriResolver.consume(any(), any())).thenReturn("bada://oauth");
    }

    @Test
    @DisplayName("처음 가입한 소셜 계정은 신규 가입으로 코드를 발급한다")
    void issuesCodeAsNewUser() throws Exception {
        givenUser(Provider.GOOGLE, "google-123");
        when(authService.issueOAuthCode(1L, true)).thenReturn("code-1");
        SocialOAuth2User principal = new SocialOAuth2User(AUTHORITIES, Map.of("sub", "google-123"), "sub", true);

        MockHttpServletResponse response = login(principal, "google");

        verify(authService).issueOAuthCode(1L, true);
        assertThat(response.getRedirectedUrl()).isEqualTo("bada://oauth?code=code-1");
    }

    @Test
    @DisplayName("OIDC(애플) 기존 회원은 신규 가입이 아닌 것으로 코드를 발급한다")
    void issuesCodeAsExistingOidcUser() throws Exception {
        givenUser(Provider.APPLE, "apple-sub-1");
        when(authService.issueOAuthCode(1L, false)).thenReturn("code-2");
        OidcIdToken idToken = new OidcIdToken(
                "id-token", Instant.now(), Instant.now().plusSeconds(60), Map.of("sub", "apple-sub-1"));
        SocialOidcUser principal = new SocialOidcUser(AUTHORITIES, idToken, null, "sub", false);

        MockHttpServletResponse response = login(principal, "apple");

        verify(authService).issueOAuthCode(1L, false);
        assertThat(response.getRedirectedUrl()).isEqualTo("bada://oauth?code=code-2");
    }

    private void givenUser(Provider provider, String providerId) {
        Users user = Users.builder().userId(1L).provider(provider).providerId(providerId).role(Role.USER).build();
        when(usersRepository.findByProviderAndProviderId(provider, providerId)).thenReturn(Optional.of(user));
    }

    private MockHttpServletResponse login(OAuth2User principal, String registrationId) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                new OAuth2AuthenticationToken(principal, principal.getAuthorities(), registrationId));
        return response;
    }
}
