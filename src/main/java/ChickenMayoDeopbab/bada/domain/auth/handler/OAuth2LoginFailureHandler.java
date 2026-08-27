package ChickenMayoDeopbab.bada.domain.auth.handler;

import ChickenMayoDeopbab.bada.domain.auth.service.OAuthRedirectUriResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final OAuthRedirectUriResolver redirectUriResolver;

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        log.warn("소셜 로그인에 실패했습니다. uri={}, query={}",
                request.getRequestURI(), request.getQueryString(), exception);

        String url = UriComponentsBuilder.fromUriString(redirectUriResolver.consume(request, response))
                .queryParam("error", "login_failed")
                .build()
                .toUriString();

        redirectStrategy.sendRedirect(request, response, url);
    }
}