package ChickenMayoDeopbab.bada.domain.auth.service;

import ChickenMayoDeopbab.bada.domain.auth.dto.request.OAuthAttributes;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOauth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final UsersRepository usersRepository;

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

        Users user = saveOrUpdate(attributes);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().getValue())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey()
        );
    }

    private Users saveOrUpdate(OAuthAttributes attributes) {
        return usersRepository.findByProviderAndProviderId(attributes.getProvider(), attributes.getProviderId())
                .map(user -> syncProfileImage(user, attributes.getPicture()))
                .orElseGet(() -> usersRepository.save(attributes.toEntity()));
    }

    // 기존 회원의 프로필 이미지는 비어 있을 때만 채운다. 앱에서 직접 바꾼 이미지를 매 로그인마다 되돌리지 않기 위함이다.
    private Users syncProfileImage(Users user, String picture) {
        return user.applyProfileImageIfAbsent(picture) ? usersRepository.save(user) : user;
    }
}