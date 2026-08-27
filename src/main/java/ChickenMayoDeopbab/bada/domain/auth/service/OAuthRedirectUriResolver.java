package ChickenMayoDeopbab.bada.domain.auth.service;

import ChickenMayoDeopbab.bada.domain.auth.exception.AuthStatusCode;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Component
public class OAuthRedirectUriResolver {

    private static final String COOKIE_NAME = "oauthRedirectUri";
    private static final Duration COOKIE_TTL = Duration.ofMinutes(5);

    private final String defaultRedirectUri;
    private final Set<String> allowedRedirectUris;

    public OAuthRedirectUriResolver(
            @Value("${app.oauth2.app-redirect-uri}") String defaultRedirectUri,
            @Value("${app.oauth2.allowed-redirect-uris:}") String allowedRedirectUris) {
        this.defaultRedirectUri = defaultRedirectUri;

        Set<String> allowed = new LinkedHashSet<>();
        allowed.add(defaultRedirectUri);
        Arrays.stream(allowedRedirectUris.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(allowed::add);
        this.allowedRedirectUris = Set.copyOf(allowed);
    }

    public void remember(
            HttpServletRequest request,
            HttpServletResponse response,
            String requestedUri) {
        String target = StringUtils.hasText(requestedUri) ? requestedUri : defaultRedirectUri;

        if (!allowedRedirectUris.contains(target)) {
            throw new ApplicationException(AuthStatusCode.UNSUPPORTED_REDIRECT_URI);
        }

        write(response, target, COOKIE_TTL, request.isSecure());
    }

    public String consume(
            HttpServletRequest request,
            HttpServletResponse response) {
        String stored = read(request);
        write(response, "", Duration.ZERO, request.isSecure());

        if (stored == null) {
            return defaultRedirectUri;
        }
        if (!allowedRedirectUris.contains(stored)) {
            log.warn("허용되지 않은 복귀 주소가 쿠키에 담겨 있어 기본값으로 대체합니다. uri={}", stored);
            return defaultRedirectUri;
        }
        return stored;
    }

    private void write(
            HttpServletResponse response,
            String value,
            Duration maxAge,
            boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, URLEncoder.encode(value, StandardCharsets.UTF_8))
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .map(value -> URLDecoder.decode(value, StandardCharsets.UTF_8))
                .findFirst()
                .orElse(null);
    }
}
