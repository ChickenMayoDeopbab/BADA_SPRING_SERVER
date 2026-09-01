package ChickenMayoDeopbab.bada.global.config;

import ChickenMayoDeopbab.bada.domain.auth.service.AppleClientSecretGenerator;
import ChickenMayoDeopbab.bada.domain.auth.service.AppleTokenRequestParametersConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;

@Configuration
@RequiredArgsConstructor
public class OAuth2ClientConfig {

    private final AppleClientSecretGenerator appleClientSecretGenerator;

    /**
     * 모든 공급자가 이 클라이언트를 공유한다. 애플 외에는 기본 동작과 동일하게 흘러가도록
     * 파라미터 변환만 갈아끼운다.
     */
    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        RestClientAuthorizationCodeTokenResponseClient client = new RestClientAuthorizationCodeTokenResponseClient();
        client.setParametersConverter(new AppleTokenRequestParametersConverter(appleClientSecretGenerator));
        return client;
    }
}
