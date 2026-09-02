package ChickenMayoDeopbab.bada.domain.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * application.yaml의 apple 등록을 그대로 옮겨 authorization 요청이 어떻게 조립되는지 확인한다.
 * 특히 authorization-uri에 박아둔 response_mode가 살아남는지가 핵심이다.
 */
class AppleAuthorizationRequestTest {

    private static final String REDIRECT_URI = "https://api.example.com/login/oauth2/code/apple";

    private final DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
            new InMemoryClientRegistrationRepository(appleRegistration()), "/oauth2/authorization");

    private static ClientRegistration appleRegistration() {
        return ClientRegistration.withRegistrationId("apple")
                .clientId("com.example.bada.web")
                .clientSecret("generated-at-runtime")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(REDIRECT_URI)
                .scope("openid", "name", "email")
                .authorizationUri("https://appleid.apple.com/auth/authorize?response_mode=form_post")
                .tokenUri("https://appleid.apple.com/auth/token")
                .jwkSetUri("https://appleid.apple.com/auth/keys")
                .userNameAttributeName("sub")
                .clientName("Apple")
                .build();
    }

    private OAuth2AuthorizationRequest resolve() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/apple");
        request.setServletPath("/oauth2/authorization/apple");
        return resolver.resolve(request);
    }

    @Test
    @DisplayName("authorization-uri에 넣은 response_mode=form_post가 최종 URI까지 살아남는다")
    void keepsResponseModeFormPost() {
        String uri = resolve().getAuthorizationRequestUri();

        assertThat(uri).contains("response_mode=form_post");
        assertThat(uri).startsWith("https://appleid.apple.com/auth/authorize?");
    }

    @Test
    @DisplayName("표준 파라미터가 response_mode와 함께 모두 실린다")
    void carriesStandardParameters() {
        String uri = resolve().getAuthorizationRequestUri();

        assertThat(uri)
                .contains("response_type=code")
                .contains("client_id=com.example.bada.web")
                .contains("state=")
                .contains("redirect_uri=");
        assertThat(uri).contains("scope=openid");
    }

    @Test
    @DisplayName("scope에 openid가 있어 Spring이 OIDC 경로를 타고 nonce를 붙인다")
    void usesOidcFlow() {
        OAuth2AuthorizationRequest request = resolve();

        // openid가 있어야 OidcAuthorizationCodeAuthenticationProvider가 선택되고,
        // 그래야 CustomOidcUserService가 호출된다.
        assertThat(request.getScopes()).contains("openid");
        assertThat(request.getAuthorizationRequestUri()).contains("nonce=");
    }

    @Test
    @DisplayName("redirect_uri는 설정값 그대로 나간다 - 애플 콘솔 Return URL과 일치해야 한다")
    void sendsConfiguredRedirectUri() {
        assertThat(resolve().getRedirectUri()).isEqualTo(REDIRECT_URI);
    }
}
