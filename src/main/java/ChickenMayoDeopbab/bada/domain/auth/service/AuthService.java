package ChickenMayoDeopbab.bada.domain.auth.service;

import ChickenMayoDeopbab.bada.domain.auth.dto.request.LoginRequest;
import ChickenMayoDeopbab.bada.domain.auth.dto.request.RefreshRequest;
import ChickenMayoDeopbab.bada.domain.auth.dto.response.TokenResponse;
import ChickenMayoDeopbab.bada.domain.auth.exception.AuthStatusCode;
import ChickenMayoDeopbab.bada.domain.user.entity.Role;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import ChickenMayoDeopbab.bada.global.jwt.JwtProvider;
import ChickenMayoDeopbab.bada.global.jwt.MemberDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UsersRepository usersRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProvider jwtProvider;
    private final BCryptPasswordEncoder passwordEncoder;

    // access는 cookie로만 반환, refresh는 redis저장 후 cookie로 반환
    public TokenResponse login(
            LoginRequest request,
            HttpServletResponse response) {
        Users user = getUser(request.username());
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApplicationException(AuthStatusCode.INVALID_PASSWORD);
        }

        String accessToken = generateAccessToken(user.getUserId(), user.getRole(), response);
        String refreshToken = generateRefreshToken(user.getUserId(), response);

        return new TokenResponse(accessToken, refreshToken);
    }

    // RefreshToken와 username을 받고 새로운 AccessToken와 RefreshToken을 반환
    public TokenResponse refresh(
            RefreshRequest request,
            HttpServletResponse response) {
        String refreshToken = redisTemplate.opsForValue().get("refreshToken: " + request.userId());

        if ( refreshToken == null || !refreshToken.equals(request.refreshToken())) {
            redisTemplate.delete("refreshToken: " + request.userId());
            throw new ApplicationException(AuthStatusCode.INVALID_REFRESH_TOKEN);
        }
        Users user = getUser(request.userId());

        String accessToken = generateAccessToken(user.getUserId(), user.getRole(), response);
        String newRefreshToken = generateRefreshToken(user.getUserId(), response);

        return new TokenResponse(accessToken, newRefreshToken);
    }

    /**
     * 로그아웃(세션 종료) 처리
     * - Redis에 저장된 RefreshToken 삭제
     * - AccessToken, RefreshToken 쿠키 제거
     */
    public void signOut(
            MemberDetails memberDetails,
            HttpServletResponse response) {
        Users user = getUser(memberDetails.getUsername());
        redisTemplate.delete("refreshToken: " + user.getUserId());

        addCookie(response, "accessToken", "", 0, true);
        addCookie(response, "refreshToken", "", 0, false);
    }

    /**
     * Access Token을 생성하고 HttpOnly 쿠키로 저장한다.
     * - 클라이언트에서는 JS로 접근 불가능
     * - 인증 요청 시 자동 포함됨
     */
    private String generateAccessToken(
            Long userId,
            Role role,
            HttpServletResponse response) {
        String accessToken = jwtProvider.createAccessToken(userId, role);

        addCookie(response, "accessToken", accessToken, 60 * 60, true);

        return accessToken;
    }

    /**
     * Refresh Token을 생성하고 Redis에 저장한 뒤 쿠키로 반환한다.
     * - Redis: 토큰 검증 및 재발급용
     * - HttpOnly=false: (필요 시 JS 접근 가능)
     */
    private String generateRefreshToken(
            Long userId,
            HttpServletResponse response) {
        String refreshToken = jwtProvider.createRefreshToken(userId);

        redisTemplate.opsForValue().set("refreshToken: " + userId, refreshToken, Duration.ofDays(7));

        addCookie(response, "refreshToken", refreshToken, 60 * 60 * 24 * 7, false);

        return refreshToken;
    }

    private void addCookie(
            HttpServletResponse response,
            String name,
            String value,
            int maxAge,
            boolean httpOnly) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(httpOnly);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    private Users getUser(
            String username) {
        return usersRepository.findByUsername(username)
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));
    }

    private Users getUser(
            Long userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));
    }
}
