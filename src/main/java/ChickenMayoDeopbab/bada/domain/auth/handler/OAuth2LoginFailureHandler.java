package ChickenMayoDeopbab.bada.domain.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * 소셜 로그인 실패 시에도 브라우저에 에러 페이지를 남기지 않고 앱으로 복귀시킨다.
 * 기본 동작({@code /login?error} 리다이렉트)은 매핑이 없어 404로 끝나므로 반드시 필요하다.
 */
@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Value("${app.oauth2.app-redirect-uri}")
    private String appRedirectUri;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        log.warn("소셜 로그인에 실패했습니다. uri={}, query={}",
                request.getRequestURI(), request.getQueryString(), exception);

        String url = UriComponentsBuilder.fromUriString(appRedirectUri)
                .queryParam("error", "login_failed")
                .build()
                .toUriString();

        redirectStrategy.sendRedirect(request, response, url);
    }
}