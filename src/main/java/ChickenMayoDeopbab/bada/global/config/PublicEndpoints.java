package ChickenMayoDeopbab.bada.global.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.List;

public final class PublicEndpoints {

    public static final String[] POST = {
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/email/send",
            "/api/v1/auth/email/check",
            "/api/v1/auth/check/username",
            "/api/v1/auth/oauth/token",
            "/api/diagnosis/submit"
    };

    public static final String[] GET = {
            "/api/v1/auth/google",
            "/api/v1/auth/naver",
            "/api/v1/auth/apple",
            "/api/diagnosis/questions"
    };

    public static final String[] ANY = {
            "/",
            "/login/**",
            "/oauth2/**",
            "/logout/*",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/internal/**"
    };

    private static final RequestMatcher MATCHER = buildMatcher();

    private PublicEndpoints() {
    }

    public static boolean matches(HttpServletRequest request) {
        return MATCHER.matches(request);
    }

    private static RequestMatcher buildMatcher() {
        List<RequestMatcher> matchers = new ArrayList<>();

        for (String pattern : POST) {
            matchers.add(new AntPathRequestMatcher(pattern, HttpMethod.POST.name()));
        }
        for (String pattern : GET) {
            matchers.add(new AntPathRequestMatcher(pattern, HttpMethod.GET.name()));
        }
        for (String pattern : ANY) {
            matchers.add(new AntPathRequestMatcher(pattern));
        }

        return new OrRequestMatcher(matchers);
    }
}
