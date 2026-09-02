package ChickenMayoDeopbab.bada.domain.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.endpoint.DefaultOAuth2TokenRequestParametersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 이 변환기는 모든 공급자의 토큰 요청을 거쳐 간다.
 * 애플 지원을 넣으면서 구글·네이버 요청까지 바뀌지 않았는지를 확인한다.
 */
class AppleTokenRequestParametersConverterTest {

    private static final String GENERATED_SECRET = "생성된-애플-client-secret";

    private final AppleClientSecretGenerator generator = mock(AppleClientSecretGenerator.class);
    private final AppleTokenRequestParametersConverter converter =
            new AppleTokenRequestParametersConverter(generator);

    @Test
    @DisplayName("애플 요청은 client_secret이 매번 생성된 JWT로 교체된다")
    void replacesClientSecretForApple() {
        when(generator.generate()).thenReturn(GENERATED_SECRET);

        MultiValueMap<String, String> parameters = converter.convert(grantRequest(appleRegistration()));

        assertThat(parameters.getFirst(OAuth2ParameterNames.CLIENT_SECRET)).isEqualTo(GENERATED_SECRET);
        // 중복 없이 정확히 하나여야 한다. 두 개면 애플이 요청을 거부한다.
        assertThat(parameters.get(OAuth2ParameterNames.CLIENT_SECRET)).hasSize(1);
        assertThat(parameters.getFirst("yaml에-박아둔-값")).isNull();
        assertThat(parameters).doesNotContainValue(java.util.List.of("generated-at-runtime"));
    }

    @Test
    @DisplayName("구글 요청은 기본 변환 결과와 완전히 동일하다")
    void leavesGoogleUntouched() {
        assertUntouched(googleRegistration());
    }

    @Test
    @DisplayName("네이버 요청은 기본 변환 결과와 완전히 동일하다")
    void leavesNaverUntouched() {
        assertUntouched(naverRegistration());
    }

    @Test
    @DisplayName("애플이 아니면 client_secret을 생성하지 않는다")
    void doesNotGenerateSecretForOtherProviders() {
        converter.convert(grantRequest(googleRegistration()));
        converter.convert(grantRequest(naverRegistration()));

        // 애플 설정이 비어 있어도 구글·네이버 로그인은 영향을 받지 않아야 한다.
        verify(generator, never()).generate();
    }

    private void assertUntouched(ClientRegistration registration) {
        OAuth2AuthorizationCodeGrantRequest request = grantRequest(registration);

        MultiValueMap<String, String> actual = converter.convert(request);
        MultiValueMap<String, String> expected =
                new DefaultOAuth2TokenRequestParametersConverter<OAuth2AuthorizationCodeGrantRequest>()
                        .convert(request);

        assertThat(actual).isEqualTo(expected);
    }

    private OAuth2AuthorizationCodeGrantRequest grantRequest(ClientRegistration registration) {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(registration.getProviderDetails().getAuthorizationUri())
                .clientId(registration.getClientId())
                .redirectUri(registration.getRedirectUri())
                .scopes(registration.getScopes())
                .state("state-123")
                .build();

        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success("code-123")
                .redirectUri(registration.getRedirectUri())
                .state("state-123")
                .build();

        return new OAuth2AuthorizationCodeGrantRequest(
                registration, new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse));
    }

    private ClientRegistration appleRegistration() {
        return base("apple")
                .clientSecret("generated-at-runtime")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .scope("openid", "name", "email")
                .authorizationUri("https://appleid.apple.com/auth/authorize?response_mode=form_post")
                .tokenUri("https://appleid.apple.com/auth/token")
                .jwkSetUri("https://appleid.apple.com/auth/keys")
                .build();
    }

    private ClientRegistration googleRegistration() {
        return base("google")
                .clientSecret("google-secret")
                .scope("profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .build();
    }

    private ClientRegistration naverRegistration() {
        return base("naver")
                .clientSecret("naver-secret")
                .scope("name", "email", "profile_image")
                .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
                .tokenUri("https://nid.naver.com/oauth2.0/token")
                .userInfoUri("https://openapi.naver.com/v1/nid/me")
                .userNameAttributeName("response")
                .build();
    }

    private ClientRegistration.Builder base(String registrationId) {
        return ClientRegistration.withRegistrationId(registrationId)
                .clientId(registrationId + "-client-id")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://api.example.com/login/oauth2/code/" + registrationId);
    }
}
