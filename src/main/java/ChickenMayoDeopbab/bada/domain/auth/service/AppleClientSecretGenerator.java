package ChickenMayoDeopbab.bada.domain.auth.service;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class AppleClientSecretGenerator {

    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";
    private static final Duration SECRET_TTL = Duration.ofDays(30);
    private static final Duration RENEW_BEFORE = Duration.ofDays(1);

    private final String teamId;
    private final String keyId;
    private final String serviceId;
    private final String privateKeyValue;

    private String cachedSecret;
    private Instant cachedExpiresAt = Instant.EPOCH;

    // .p8이 아직 주입되지 않아도 애플리케이션은 기동되어야 하므로 검증은 실제 사용 시점으로 미룬다.
    public AppleClientSecretGenerator(
            @Value("${app.oauth2.apple.team-id:}") String teamId,
            @Value("${app.oauth2.apple.key-id:}") String keyId,
            @Value("${app.oauth2.apple.service-id:}") String serviceId,
            @Value("${app.oauth2.apple.private-key:}") String privateKeyValue) {
        this.teamId = teamId;
        this.keyId = keyId;
        this.serviceId = serviceId;
        this.privateKeyValue = privateKeyValue;
    }

    public synchronized String generate() {
        if (cachedSecret != null && Instant.now().isBefore(cachedExpiresAt.minus(RENEW_BEFORE))) {
            return cachedSecret;
        }

        requireConfigured();

        Instant now = Instant.now();
        Instant expiresAt = now.plus(SECRET_TTL);

        cachedSecret = Jwts.builder()
                .header().keyId(keyId).and()
                .issuer(teamId)
                .claim("aud", APPLE_AUDIENCE)
                .subject(serviceId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(toPrivateKey(privateKeyValue), Jwts.SIG.ES256)
                .compact();
        cachedExpiresAt = expiresAt;

        log.info("Apple client_secret을 재발급했습니다. 만료={}", expiresAt);
        return cachedSecret;
    }

    private void requireConfigured() {
        if (!StringUtils.hasText(teamId) || !StringUtils.hasText(keyId)
                || !StringUtils.hasText(serviceId) || !StringUtils.hasText(privateKeyValue)) {
            throw new IllegalStateException(
                    "Apple 로그인 설정이 비어 있습니다. APPLE_TEAM_ID / APPLE_KEY_ID / APPLE_SERVICE_ID / APPLE_PRIVATE_KEY를 확인하세요.");
        }
    }

    // .p8은 PEM 헤더가 붙은 형태로도, base64 본문만 한 줄로도 들어올 수 있어 둘 다 받는다.
    private PrivateKey toPrivateKey(String raw) {
        String base64 = raw.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        try {
            byte[] der = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Apple .p8 개인키를 읽을 수 없습니다. APPLE_PRIVATE_KEY 값을 확인하세요.", e);
        }
    }
}
