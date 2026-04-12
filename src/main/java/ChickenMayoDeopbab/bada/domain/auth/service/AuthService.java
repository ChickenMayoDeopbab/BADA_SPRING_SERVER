package ChickenMayoDeopbab.bada.domain.auth.service;

import ChickenMayoDeopbab.bada.domain.auth.dto.request.LoginRequest;
import ChickenMayoDeopbab.bada.domain.auth.dto.response.LoginResponse;
import ChickenMayoDeopbab.bada.domain.auth.exception.AuthStatusCode;
import ChickenMayoDeopbab.bada.domain.user.entity.Role;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import ChickenMayoDeopbab.bada.global.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UsersRepository usersRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProvider jwtProvider;
    private final BCryptPasswordEncoder passwordEncoder;

    public LoginResponse login(
            LoginRequest request,
            HttpServletResponse response) {
        Users user = usersRepository.findByUsername(request.username())
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApplicationException(AuthStatusCode.INVALID_PASSWORD);
        }

        String accessToken = generateAccessToken(user.getUsername(), user.getRole(), response);
        String refreshToken = generateRefreshToken(user.getUsername(), response);

        return new LoginResponse(accessToken, refreshToken);
    }

    private String generateAccessToken(
            String username,
            Role role,
            HttpServletResponse response) {
        String accessToken = jwtProvider.createAccessToken(username, role);

        Cookie cookie = new Cookie("accessToken", accessToken);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(3600);
        response.addCookie(cookie);

        return accessToken;
    }

    private String generateRefreshToken(
            String username,
            HttpServletResponse response) {
        String refreshToken = jwtProvider.createRefreshToken(username);

        redisTemplate.opsForValue().set("refreshToken: " + username, refreshToken);

        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setPath("/");
        cookie.setHttpOnly(false);
        cookie.setMaxAge(3600);
        response.addCookie(cookie);

        return refreshToken;
    }
}
