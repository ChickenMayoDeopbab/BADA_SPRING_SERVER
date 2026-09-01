package ChickenMayoDeopbab.bada.domain.auth.service;

import ChickenMayoDeopbab.bada.domain.auth.dto.request.OAuthAttributes;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private static final String APPLE_REGISTRATION_ID = "apple";

    private final OAuthUserRegistrar registrar;
    private final ObjectMapper objectMapper;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        OAuthAttributes attributes = APPLE_REGISTRATION_ID.equals(registrationId)
                ? OAuthAttributes.ofApple(nameAttributeKey, oidcUser.getClaims(), readAppleName())
                : OAuthAttributes.of(registrationId, nameAttributeKey, oidcUser.getClaims());

        Users user;
        try {
            user = registrar.register(attributes);
        } catch (Exception e) {
            log.error("소셜 회원 저장에 실패했습니다. registrationId={}", registrationId, e);
            throw new OAuth2AuthenticationException(new OAuth2Error("user_registration_failed"), e);
        }

        return new DefaultOidcUser(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().getValue())),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                nameAttributeKey
        );
    }
    
    private String readAppleName() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        String userJson = attributes.getRequest().getParameter("user");
        if (!StringUtils.hasText(userJson)) {
            return null;
        }

        try {
            JsonNode name = objectMapper.readTree(userJson).path("name");
            String fullName = (name.path("lastName").asText("") + name.path("firstName").asText("")).trim();
            return StringUtils.hasText(fullName) ? fullName : null;
        } catch (Exception e) {
            log.warn("Apple user 파라미터를 해석하지 못했습니다.", e);
            return null;
        }
    }
}
