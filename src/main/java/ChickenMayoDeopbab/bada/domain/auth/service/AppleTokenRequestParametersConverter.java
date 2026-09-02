package ChickenMayoDeopbab.bada.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.client.endpoint.DefaultOAuth2TokenRequestParametersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.MultiValueMap;

/**
 * 애플만 client_secret이 고정 문자열이 아니라 매번 새로 서명한 JWT여야 한다.
 * 토큰 요청 직전에 그 자리만 갈아끼우고, 나머지 공급자의 요청은 기본 변환 결과를 그대로 내보낸다.
 */
@RequiredArgsConstructor
public class AppleTokenRequestParametersConverter
        implements Converter<OAuth2AuthorizationCodeGrantRequest, MultiValueMap<String, String>> {

    private static final String APPLE_REGISTRATION_ID = "apple";

    private final Converter<OAuth2AuthorizationCodeGrantRequest, MultiValueMap<String, String>> delegate =
            new DefaultOAuth2TokenRequestParametersConverter<>();

    private final AppleClientSecretGenerator appleClientSecretGenerator;

    @Override
    public MultiValueMap<String, String> convert(OAuth2AuthorizationCodeGrantRequest request) {
        MultiValueMap<String, String> parameters = delegate.convert(request);

        if (APPLE_REGISTRATION_ID.equals(request.getClientRegistration().getRegistrationId())) {
            parameters.set(OAuth2ParameterNames.CLIENT_SECRET, appleClientSecretGenerator.generate());
        }
        return parameters;
    }
}
