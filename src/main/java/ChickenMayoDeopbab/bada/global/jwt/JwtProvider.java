package ChickenMayoDeopbab.bada.global.jwt;

import ChickenMayoDeopbab.bada.domain.user.entity.Role;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtProvider(@Value("${app.jwt.secret}")String secretKey, @Value("${app.jwt.refresh-expiration}") long refreshExpiration, @Value("${app.jwt.access-expiration}") long accessExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String createAccessToken(String username, Role role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessExpiration);

        return Jwts.builder()
                .subject("ACCESS")
                .claim("username", username)
                .claim("role", role.getValue())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject("REFRESH")
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseClaims(token);

        return claims.get("username").toString();
    }

    public String getSubjectFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    public void validateToken(String token) {
        try {
            parseClaims(token);
        } catch (ExpiredJwtException e) {
            throw ApplicationException.of(JwtStatusCode.TOKEN_EXPIRED);
        } catch (MalformedJwtException e) {
            throw ApplicationException.of(JwtStatusCode.TOKEN_MALFORMED);
        } catch (SignatureException e) {
            throw ApplicationException.of(JwtStatusCode.TOKEN_INVALID);
        }
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseClaimsJws(token)
                .getPayload();
    }
}