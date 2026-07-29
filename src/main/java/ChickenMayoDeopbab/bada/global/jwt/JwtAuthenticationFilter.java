package ChickenMayoDeopbab.bada.global.jwt;

import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.common.ErrorResponse;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import ChickenMayoDeopbab.bada.global.exception.statuscode.JwtStatusCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;
    private final JwtProvider jwtProvider;
    private final MemberDetailsService memberDetailsService;

    // 인증 없이 접근하는 경로. 만료된 accessToken 쿠키가 남아 있어도 401로 끊기지 않아야 한다.
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/check/username",
            "/api/v1/auth/email/send",
            "/api/v1/auth/email/check",
            "/api/v1/auth/oauth/token",
            "/api/v1/auth/google",
            "/api/v1/auth/naver"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || PUBLIC_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String token = jwtProvider.resolveToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwtProvider.validateToken(token);
        } catch (ApplicationException e) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            ApiResponse<Void> body = ApiResponse.error(
                    HttpStatus.UNAUTHORIZED,
                    ErrorResponse.of(e.getStatusCode().getCode(), e.getMessage())
            );
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        if (jwtProvider.getTypeFromToken(token).filter("ACCESS"::equals).isEmpty()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            ApiResponse<Void> body = ApiResponse.error(
                    HttpStatus.UNAUTHORIZED,
                    ErrorResponse.of(JwtStatusCode.TOKEN_INVALID.getCode(), JwtStatusCode.TOKEN_INVALID.getMessage())
            );
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        Long userId = jwtProvider.getUserIdFromToken(token);
        UserDetails userDetails = memberDetailsService.loadUserById(userId);
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        filterChain.doFilter(request, response);
    }
}
