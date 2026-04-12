package ChickenMayoDeopbab.bada.global.jwt;

import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.common.ErrorResponse;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
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

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;
    private final JwtProvider jwtProvider;
    private final MemberDetailsService memberDetailsService;

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

        if ("REFRESH".equals(jwtProvider.getSubjectFromToken(token))) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            ApiResponse<Void> body = ApiResponse.error(
                    HttpStatus.UNAUTHORIZED,
                    ErrorResponse.of(JwtStatusCode.TOKEN_INVALID.getCode(), JwtStatusCode.TOKEN_INVALID.getMessage())
            );
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        String username = jwtProvider.getUsernameFromToken(token);
        UserDetails userDetails = memberDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        filterChain.doFilter(request, response);
    }
}
