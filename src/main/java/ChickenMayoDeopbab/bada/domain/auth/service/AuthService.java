package ChickenMayoDeopbab.bada.domain.auth.service;

import ChickenMayoDeopbab.bada.domain.auth.dto.request.ChangePasswordRequest;
import ChickenMayoDeopbab.bada.domain.auth.dto.request.CheckUsernameRequest;
import ChickenMayoDeopbab.bada.domain.auth.dto.request.LoginRequest;
import ChickenMayoDeopbab.bada.domain.auth.dto.request.RefreshRequest;
import ChickenMayoDeopbab.bada.domain.auth.dto.response.TokenResponse;
import ChickenMayoDeopbab.bada.domain.auth.exception.AuthStatusCode;
import ChickenMayoDeopbab.bada.domain.user.entity.Provider;
import ChickenMayoDeopbab.bada.domain.user.entity.Role;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import ChickenMayoDeopbab.bada.global.jwt.JwtProvider;
import ChickenMayoDeopbab.bada.global.jwt.MemberDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class AuthService {

    private static final String OAUTH_CODE_PREFIX = "oauthCode: ";
    private static final Duration OAUTH_CODE_TTL = Duration.ofSeconds(60);

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

    public void checkUsername(CheckUsernameRequest request) {
        if (usersRepository.existsByUsername(request.username())) {
            throw new ApplicationException(AuthStatusCode.ALREADY_EXIST_USERNAME);
        }
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
     * 소셜 로그인 성공 직후 1회용 교환 코드를 발급한다.
     * - 딥링크 URL에 토큰 대신 이 코드만 실어 앱으로 돌려보낸다.
     * - Redis에 짧은 TTL로 저장하며, 교환 시 즉시 삭제되어 재사용할 수 없다.
     */
    public String issueOAuthCode(Long userId) {
        String code = UUID.randomUUID().toString();

        redisTemplate.opsForValue()
                .set(OAUTH_CODE_PREFIX + code, String.valueOf(userId), OAUTH_CODE_TTL);

        return code;
    }

    /**
     * 1회용 교환 코드를 AccessToken/RefreshToken으로 교환한다.
     * - 코드는 조회와 동시에 삭제되므로 두 번째 요청은 실패한다.
     */
    public TokenResponse exchangeOAuthCode(
            String code,
            HttpServletResponse response) {
        String userId = redisTemplate.opsForValue().getAndDelete(OAUTH_CODE_PREFIX + code);

        if (userId == null) {
            throw new ApplicationException(AuthStatusCode.INVALID_OAUTH_CODE);
        }

        Users user = getUser(Long.valueOf(userId));

        String accessToken = generateAccessToken(user.getUserId(), user.getRole(), response);
        String refreshToken = generateRefreshToken(user.getUserId(), response);

        return new TokenResponse(accessToken, refreshToken);
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

    public ApiResponse<String> findId(String email) {
        validateEmailVerified(email);
        Users user = usersRepository.findByEmailAndProvider(email,Provider.LOCAL)
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));

        return ApiResponse.ok(user.getUsername(), "아이디를 조회했습니다.");
    }

    public ApiResponse<?> changePassword(ChangePasswordRequest request) {
        validateEmailVerified(request.email());
        Users user = usersRepository.findByEmailAndProvider(request.email(), Provider.LOCAL)
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));

        validatePassword(request.oldPassword(), request.newPassword(), user);

        user.setPassword(passwordEncoder.encode(request.newPassword()));

        return ApiResponse.ok("비밀번호가 변경되었습니다.");

    }

    /**
     * Access Token을 생성하고 HttpOnly 쿠키로 저장한다.
     * - 클라이언트에서는 JS로 접근 불가능
     * - 인증 요청 시 자동 포함됨
     */
    public String generateAccessToken(
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
    public String generateRefreshToken(
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

    private void validateEmailVerified(String email) {
        String status = redisTemplate.opsForValue().get(email);
        if (!"ACCESS".equals(status)) {
            throw new ApplicationException(UsersStatusCode.EMAIL_NOT_VERIFIED);
        }
    }

    private void validatePassword(String oldPassword, String newPassword, Users user) {
        // 예전 비밀번호와 user의 비밀번호가 맞는 지 확인
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new ApplicationException(AuthStatusCode.INVALID_PASSWORD);
        }

        // 변경하려는 비밀번호와 현재 비밀번호가 같은 지 확인
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new ApplicationException(AuthStatusCode.SAME_AS_CURRENT_PASSWORD);
        }

    }
}
