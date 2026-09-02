package ChickenMayoDeopbab.bada.domain.auth.handler;

import ChickenMayoDeopbab.bada.domain.auth.service.AuthService;
import ChickenMayoDeopbab.bada.domain.auth.service.OAuthRedirectUriResolver;
import ChickenMayoDeopbab.bada.domain.user.entity.Provider;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final UsersRepository usersRepository;
    private final OAuthRedirectUriResolver redirectUriResolver;

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        String redirectUri = redirectUriResolver.consume(request, response);

        String code;
        try {
            code = issueCode(authentication);
        } catch (Exception e) {
            log.error("소셜 로그인 후처리에 실패했습니다.", e);
            redirectStrategy.sendRedirect(request, response, buildRedirectUrl(redirectUri, "error", "login_failed"));
            return;
        }

        redirectStrategy.sendRedirect(request, response, buildRedirectUrl(redirectUri, "code", code));
    }

    private String issueCode(Authentication authentication) {
        // 이메일은 미제공(네이버 선택 동의 등)일 수 있으므로 provider 고유 식별자로 매칭한다.
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String providerId = oAuth2User.getName();

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        Provider provider = Provider.valueOf(authToken.getAuthorizedClientRegistrationId().toUpperCase());

        Users user = usersRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));

        return authService.issueOAuthCode(user.getUserId());
    }

    private String buildRedirectUrl(String redirectUri, String paramName, String paramValue) {
        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam(paramName, paramValue)
                .build()
                .toUriString();
    }
}