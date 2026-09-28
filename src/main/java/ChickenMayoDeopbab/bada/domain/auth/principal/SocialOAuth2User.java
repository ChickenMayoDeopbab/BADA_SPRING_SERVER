package ChickenMayoDeopbab.bada.domain.auth.principal;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;

public class SocialOAuth2User extends DefaultOAuth2User implements SocialLoginUser {

    private final boolean newUser;

    public SocialOAuth2User(
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> attributes,
            String nameAttributeKey,
            boolean newUser) {
        super(authorities, attributes, nameAttributeKey);
        this.newUser = newUser;
    }

    @Override
    public boolean isNewUser() {
        return newUser;
    }
}
