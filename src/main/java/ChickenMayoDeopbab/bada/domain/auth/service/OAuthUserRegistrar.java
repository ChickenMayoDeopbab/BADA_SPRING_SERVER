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

    public Registration register(OAuthAttributes attributes) {
        return usersRepository.findByProviderAndProviderId(attributes.getProvider(), attributes.getProviderId())
                .map(user -> new Registration(syncProfileImage(user, attributes.getPicture()), false))
                .orElseGet(() -> new Registration(usersRepository.save(attributes.toEntity()), true));
    }

    // 기존 회원의 프로필 이미지는 비어 있을 때만 채운다. 앱에서 직접 바꾼 이미지를 매 로그인마다 되돌리지 않기 위함이다.
    private Users syncProfileImage(Users user, String picture) {
        if (!user.isProfileImageAgreed()) {
            return user;
        }
        return user.applyProfileImageIfAbsent(picture) ? usersRepository.save(user) : user;
    }

    // newUser: 이번 로그인에서 계정이 새로 만들어졌는지. 앱이 첫 가입 화면을 띄울지 판단하는 데 쓴다.
    public record Registration(Users user, boolean newUser) {
    }
}
