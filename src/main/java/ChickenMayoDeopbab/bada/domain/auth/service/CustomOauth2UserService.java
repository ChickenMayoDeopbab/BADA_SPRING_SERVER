package ChickenMayoDeopbab.bada.domain.auth.service;

import ChickenMayoDeopbab.bada.domain.auth.dto.request.OAuthAttributes;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOauth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuthUserRegistrar registrar;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(
                registrationId,
                userNameAttributeName,
                oAuth2User.getAttributes());

        // 저장 실패(네이버 이메일 미동의로 인한 제약 위반 등)를 그대로 던지면 AuthenticationException이
        // 아니라서 failureHandler를 타지 못하고 500 페이지로 끝난다. 앱으로 돌려보내려면 변환해야 한다.
        Users user;
        try {
            user = registrar.register(attributes);
        } catch (Exception e) {
            log.error("소셜 회원 저장에 실패했습니다. registrationId={}", registrationId, e);
            throw new OAuth2AuthenticationException(new OAuth2Error("user_registration_failed"), e);
        }

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().getValue())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey()
        );
    }
}
