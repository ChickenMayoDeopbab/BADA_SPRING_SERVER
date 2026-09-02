package ChickenMayoDeopbab.bada.domain.auth.service;

import ChickenMayoDeopbab.bada.domain.auth.dto.request.OAuthAttributes;
import ChickenMayoDeopbab.bada.domain.user.entity.Provider;
import ChickenMayoDeopbab.bada.domain.user.entity.Role;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 애플을 넣으면서 구글·네이버가 쓰던 회원 저장 로직을 이 클래스로 분리했다.
 * 분리 전 동작이 그대로인지 확인한다.
 */
class OAuthUserRegistrarTest {

    private final UsersRepository usersRepository = mock(UsersRepository.class);
    private final OAuthUserRegistrar registrar = new OAuthUserRegistrar(usersRepository);

    private OAuthAttributes googleAttributes(String picture) {
        return OAuthAttributes.of("google", "sub",
                Map.of("sub", "google-123", "name", "홍길동", "email", "a@b.com", "picture", picture));
    }

    @Test
    @DisplayName("처음 보는 소셜 계정은 새로 저장한다")
    void savesNewUser() {
        when(usersRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-123"))
                .thenReturn(Optional.empty());
        when(usersRepository.save(any(Users.class))).thenAnswer(i -> i.getArgument(0));

        Users saved = registrar.register(googleAttributes("https://img/1.png"));

        assertThat(saved.getProvider()).isEqualTo(Provider.GOOGLE);
        assertThat(saved.getProviderId()).isEqualTo("google-123");
        assertThat(saved.getEmail()).isEqualTo("a@b.com");
        assertThat(saved.getUsername()).startsWith("USER_");
    }

    @Test
    @DisplayName("이미 가입한 계정은 다시 저장하지 않는다")
    void reusesExistingUser() {
        Users existing = Users.builder()
                .userId(1L).provider(Provider.GOOGLE).providerId("google-123")
                .profileImage("https://img/기존.png").role(Role.USER).build();
        when(usersRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(existing));

        Users result = registrar.register(googleAttributes("https://img/새것.png"));

        assertThat(result).isSameAs(existing);
        // 앱에서 바꾼 프로필 이미지를 매 로그인마다 되돌리면 안 된다.
        assertThat(result.getProfileImage()).isEqualTo("https://img/기존.png");
        verify(usersRepository, never()).save(any(Users.class));
    }

    @Test
    @DisplayName("기존 회원의 프로필 이미지가 비어 있을 때만 채운다")
    void fillsProfileImageOnlyWhenAbsent() {
        Users existing = Users.builder()
                .userId(1L).provider(Provider.GOOGLE).providerId("google-123")
                .profileImage(null).role(Role.USER).build();
        when(usersRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(existing));
        when(usersRepository.save(any(Users.class))).thenAnswer(i -> i.getArgument(0));

        Users result = registrar.register(googleAttributes("https://img/새것.png"));

        assertThat(result.getProfileImage()).isEqualTo("https://img/새것.png");
        verify(usersRepository).save(existing);
    }

    @Test
    @DisplayName("애플처럼 프로필 이미지가 없는 공급자도 그대로 처리된다")
    void handlesProviderWithoutPicture() {
        when(usersRepository.findByProviderAndProviderId(Provider.APPLE, "apple-sub-1"))
                .thenReturn(Optional.empty());
        when(usersRepository.save(any(Users.class))).thenAnswer(i -> i.getArgument(0));

        OAuthAttributes attributes = OAuthAttributes.ofApple(
                "sub", Map.of("sub", "apple-sub-1", "email", "hidden@privaterelay.appleid.com"), "홍길동");

        Users saved = registrar.register(attributes);

        assertThat(saved.getProvider()).isEqualTo(Provider.APPLE);
        assertThat(saved.getProviderId()).isEqualTo("apple-sub-1");
        assertThat(saved.getName()).isEqualTo("홍길동");
        assertThat(saved.getProfileImage()).isNull();
    }

    @Test
    @DisplayName("이메일을 주지 않는 공급자도 저장할 수 있다")
    void savesUserWithoutEmail() {
        when(usersRepository.findByProviderAndProviderId(Provider.APPLE, "apple-sub-2"))
                .thenReturn(Optional.empty());
        when(usersRepository.save(any(Users.class))).thenAnswer(i -> i.getArgument(0));

        Users saved = registrar.register(
                OAuthAttributes.ofApple("sub", Map.of("sub", "apple-sub-2"), null));

        assertThat(saved.getEmail()).isNull();
        assertThat(saved.getProviderId()).isEqualTo("apple-sub-2");
    }
}
