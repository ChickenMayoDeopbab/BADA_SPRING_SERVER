package ChickenMayoDeopbab.bada.global.jwt;

import ChickenMayoDeopbab.bada.domain.user.entity.Role;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refreshToken: ";

    public String generateAccessToken(Long userId, Role role, HttpServletResponse response) {
        String accessToken = jwtProvider.createAccessToken(userId, role);
        addCookie(response, "accessToken", accessToken, 60 * 60, true);
        return accessToken;
    }

    public String generateRefreshToken(Long userId, HttpServletResponse response) {
        String refreshToken = jwtProvider.createRefreshToken(userId);
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + userId, refreshToken, Duration.ofDays(7));
        addCookie(response, "refreshToken", refreshToken, 60 * 60 * 24 * 7, false);
        return refreshToken;
    }

    public String getStoredRefreshToken(Long userId) {
        return redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
    }

    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }

    public void clearTokenCookies(HttpServletResponse response) {
        addCookie(response, "accessToken", "", 0, true);
        addCookie(response, "refreshToken", "", 0, false);
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge, boolean httpOnly) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(httpOnly);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
}