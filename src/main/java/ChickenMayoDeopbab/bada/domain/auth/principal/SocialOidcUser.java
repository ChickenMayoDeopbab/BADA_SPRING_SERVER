package ChickenMayoDeopbab.bada.domain.auth.principal;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Collection;

public class SocialOidcUser extends DefaultOidcUser implements SocialLoginUser {

    private final boolean newUser;

    public SocialOidcUser(
            Collection<? extends GrantedAuthority> authorities,
            OidcIdToken idToken,
            OidcUserInfo userInfo,
            String nameAttributeKey,
            boolean newUser) {
        super(authorities, idToken, userInfo, nameAttributeKey);
        this.newUser = newUser;
    }

    @Override
    public boolean isNewUser() {
        return newUser;
    }
}
