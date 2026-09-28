package ChickenMayoDeopbab.bada.global.jwt;

import ChickenMayoDeopbab.bada.domain.user.entity.Role;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import ChickenMayoDeopbab.bada.global.exception.statuscode.JwtStatusCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

@Component
@Slf4j
public class JwtProvider {

    private static final String RECORDING_TYPE = "RECORDING";
    private static final Duration RECORDING_TTL = Duration.ofMinutes(10);

    private final SecretKey secretKey;
    @Getter
    private final long accessExpiration;
    @Getter
    private final long refreshExpiration;

    public JwtProvider(
            @Value("${app.jwt.secret}") String secretKey,
            @Value("${app.jwt.access-expiration}") long accessExpiration,
            @Value("${app.jwt.refresh-expiration}") long refreshExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String createAccessToken(Long userId, Role role) {
        return buildToken(userId, "ACCESS", accessExpiration, role);
    }

    public String createRefreshToken(Long userId) {
        return buildToken(userId, "REFRESH", refreshExpiration, null);
    }

    private String buildToken(Long userId, String type, long ttl, Role role) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl))
                .signWith(secretKey);
        if (role != null) {
            builder.claim("role", role.getValue());
        }
        return builder.compact();
    }

    // 녹음 재생 링크용. type 이 ACCESS 가 아니라서 일반 API 인증에는 쓰이지 않는다.
    public String createRecordingToken(Long userId, Long recordId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", RECORDING_TYPE)
                .claim("rid", recordId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + RECORDING_TTL.toMillis()))
                .signWith(secretKey)
                .compact();
    }

    public RecordingGrant parseRecordingToken(String token) {
        validateToken(token);
        Claims claims = parseClaims(token);
        Object recordId = claims.get("rid");
        if (!RECORDING_TYPE.equals(claims.get("type")) || !(recordId instanceof Number)) {
            throw ApplicationException.of(JwtStatusCode.TOKEN_INVALID);
        }
        return new RecordingGrant(Long.parseLong(claims.getSubject()), ((Number) recordId).longValue());
    }

    public record RecordingGrant(Long userId, Long recordId) {
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);

        return Long.parseLong(claims.getSubject());
    }

    public Optional<String> getTypeFromToken(String token) {
        Claims claims = parseClaims(token);

        return Optional.ofNullable(claims.get("type")).map(Object::toString);
    }

    public void validateToken(String token) {
        try {
            parseClaims(token);
        } catch (ExpiredJwtException e) {
            throw ApplicationException.of(JwtStatusCode.TOKEN_EXPIRED);
        } catch (MalformedJwtException e) {
            throw ApplicationException.of(JwtStatusCode.TOKEN_MALFORMED);
        } catch (JwtException | IllegalArgumentException e) {
            throw ApplicationException.of(JwtStatusCode.TOKEN_INVALID);
        }
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        return Arrays.stream(cookies)
                .filter(c -> "accessToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}