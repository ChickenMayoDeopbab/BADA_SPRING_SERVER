package ChickenMayoDeopbab.bada.domain.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppleClientSecretGeneratorTest {

    private static final String TEAM_ID = "ABCDE12345";
    private static final String KEY_ID = "22X9UQ6N6P";
    private static final String SERVICE_ID = "com.example.bada.web";

    private final KeyPair keyPair = generateEcKeyPair();
    private final String base64PrivateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

    private static KeyPair generateEcKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private AppleClientSecretGenerator generator(String privateKey) {
        return new AppleClientSecretGenerator(TEAM_ID, KEY_ID, SERVICE_ID, privateKey);
    }

    @Test
    @DisplayName("애플이 요구하는 클레임 구성으로 ES256 서명된 client_secret을 만든다")
    void generatesSignedSecretWithAppleClaims() {
        String secret = generator(base64PrivateKey).generate();

        Jws<Claims> jws = Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .build()
                .parseSignedClaims(secret);

        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("ES256");
        assertThat(jws.getHeader().getKeyId()).isEqualTo(KEY_ID);

        Claims claims = jws.getPayload();
        assertThat(claims.getIssuer()).isEqualTo(TEAM_ID);
        assertThat(claims.getSubject()).isEqualTo(SERVICE_ID);
        assertThat(claims.getAudience()).contains("https://appleid.apple.com");
        assertThat(claims.getExpiration().toInstant())
                .isAfter(Instant.now())
                .isBefore(Instant.now().plus(Duration.ofDays(180)));   // 애플 상한 6개월
    }

    @Test
    @DisplayName("aud는 배열이 아니라 문자열로 직렬화된다")
    void serializesAudienceAsString() {
        String secret = generator(base64PrivateKey).generate();
        String payload = new String(Base64.getUrlDecoder().decode(secret.split("\\.")[1]));

        assertThat(payload).contains("\"aud\":\"https://appleid.apple.com\"");
    }

    @Test
    @DisplayName("PEM 헤더와 줄바꿈이 붙은 .p8 원본도 그대로 읽는다")
    void acceptsRawPemForm() {
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + String.join("\n", base64PrivateKey.split("(?<=\\G.{64})"))
                + "\n-----END PRIVATE KEY-----\n";

        String secret = generator(pem).generate();

        assertThat(Jwts.parser().verifyWith(keyPair.getPublic()).build().parseSignedClaims(secret)).isNotNull();
    }

    @Test
    @DisplayName("만료 전에는 같은 secret을 재사용한다")
    void cachesUntilExpiry() {
        AppleClientSecretGenerator generator = generator(base64PrivateKey);

        assertThat(generator.generate()).isEqualTo(generator.generate());
    }

    @Test
    @DisplayName("설정이 비어 있으면 기동이 아니라 사용 시점에 실패한다")
    void failsLazilyWhenNotConfigured() {
        AppleClientSecretGenerator generator = new AppleClientSecretGenerator("", "", "", "");

        assertThatThrownBy(generator::generate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPLE_TEAM_ID");
    }

    @Test
    @DisplayName("개인키가 잘못되면 명확한 메시지로 실패한다")
    void failsClearlyOnBrokenKey() {
        assertThatThrownBy(() -> generator("not-a-real-key").generate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPLE_PRIVATE_KEY");
    }
}
