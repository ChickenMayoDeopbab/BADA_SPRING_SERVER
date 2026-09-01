package ChickenMayoDeopbab.bada.domain.auth.dto.request;

import ChickenMayoDeopbab.bada.domain.user.entity.Provider;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
public class OAuthAttributes {

    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    private final String name;
    private final String email;
    private final String picture;
    private final Provider provider;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey,
                           String name, String email, String picture, Provider provider) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.provider = provider != null ? provider : Provider.GOOGLE;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName,
                                     Map<String, Object> attributes) {
        if ("naver".equals(registrationId)) {
            return ofNaver(userNameAttributeName, attributes);
        }
        return OAuthAttributes.builder()
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .picture((String) attributes.get("picture"))
                .provider(Provider.valueOf(registrationId.toUpperCase()))
                .build();
    }

    public String getProviderId() {
        return String.valueOf(attributes.get(nameAttributeKey));
    }

    public Users toEntity() {
        return Users.builder()
                .username("USER_" + UUID.randomUUID().toString().substring(0, 8))
                .email(email)
                .name(name)
                .profileImage(picture)
                .provider(provider)
                .providerId(getProviderId())
                .build();
    }

    /**
     * 애플은 id_token 클레임에 sub / email 정도만 담겨 온다.
     * 프로필 이미지는 제공하지 않고, 이름은 콜백 폼의 user 파라미터에서 따로 읽어 넘겨받는다.
     */
    public static OAuthAttributes ofApple(String userNameAttributeName,
                                          Map<String, Object> claims,
                                          String name) {
        return OAuthAttributes.builder()
                .attributes(claims)
                .nameAttributeKey(userNameAttributeName != null ? userNameAttributeName : "sub")
                .name(name)
                .email((String) claims.get("email"))
                .picture(null)
                .provider(Provider.APPLE)
                .build();
    }

    public static OAuthAttributes ofNaver(String userNameAttributeName,
                                          Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");


        return OAuthAttributes.builder()
                .attributes(response)
                .nameAttributeKey("id")
                .name((String) response.get("name"))
                .email((String) response.get("email"))
                .picture((String) response.get("profile_image"))
                .provider(Provider.NAVER)
                .build();
    }
}
