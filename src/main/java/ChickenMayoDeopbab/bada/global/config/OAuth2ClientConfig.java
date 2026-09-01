package ChickenMayoDeopbab.bada.global.config;

import ChickenMayoDeopbab.bada.domain.auth.service.AppleClientSecretGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.endpoint.DefaultOAuth2TokenRequestParametersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.MultiValueMap;

@Configuration
@RequiredArgsConstructor
public class OAuth2ClientConfig {

    private static final String APPLE_REGISTRATION_ID = "apple";

    private final AppleClientSecretGenerator appleClientSecretGenerator;

    /**
     * 애플만 client_secret이 매번 새로 서명된 JWT여야 하므로 토큰 요청 파라미터를 갈아끼운다.
     * 구글/네이버는 기본 변환 결과를 그대로 내보낸다.
     */
    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        DefaultOAuth2TokenRequestParametersConverter<OAuth2AuthorizationCodeGrantRequest> defaultConverter =
                new DefaultOAuth2TokenRequestParametersConverter<>();

        RestClientAuthorizationCodeTokenResponseClient client = new RestClientAuthorizationCodeTokenResponseClient();
        client.setParametersConverter(request -> {
            MultiValueMap<String, String> parameters = defaultConverter.convert(request);
            if (APPLE_REGISTRATION_ID.equals(request.getClientRegistration().getRegistrationId())) {
                parameters.set(OAuth2ParameterNames.CLIENT_SECRET, appleClientSecretGenerator.generate());
            }
            return parameters;
        });
        return client;
    }
}
