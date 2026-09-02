package ChickenMayoDeopbab.bada.domain.auth.service;

import ChickenMayoDeopbab.bada.domain.auth.dto.request.OAuthAttributes;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuthUserRegistrar {

    private final UsersRepository usersRepository;

    public Users register(OAuthAttributes attributes) {
        return usersRepository.findByProviderAndProviderId(attributes.getProvider(), attributes.getProviderId())
                .map(user -> syncProfileImage(user, attributes.getPicture()))
                .orElseGet(() -> usersRepository.save(attributes.toEntity()));
    }

    // 기존 회원의 프로필 이미지는 비어 있을 때만 채운다. 앱에서 직접 바꾼 이미지를 매 로그인마다 되돌리지 않기 위함이다.
    private Users syncProfileImage(Users user, String picture) {
        return user.applyProfileImageIfAbsent(picture) ? usersRepository.save(user) : user;
    }
}
