package ChickenMayoDeopbab.bada.domain.trainingrecord.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.InvalidCiphertextException;

import java.util.Base64;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecordingCipherTest {

    // FastAPI recording_crypto.encrypt 로 만든 값. 데이터 키 0x00..0x1f, 잠긴 데이터 키 "test-edk".
    private static final String KEY = "recordings/sess-1/a.wav";
    private static final byte[] WAV = Base64.getDecoder().decode(
            "UklGRigAAABXQVZFZm10IBAAAAABAAEAgD4AAAB9AAACABAAZGF0YQQAAAABAAIA");
    private static final byte[] BLOB = Base64.getDecoder().decode(
            "QkFEQVJFQzEACHRlc3QtZWRrtuWwowai9GutaiRp5C489dEk8qIL+geLlks5ul2/3QDMu8nHy+tYwgdAuDKJcaoULiaGMoXajkrsz9P8Yt/sApNelPYY9OQtMhsyYw==");

    private final KmsClient kmsClient = mock(KmsClient.class);
    private final RecordingCipher cipher = new RecordingCipher(kmsClient);

    @BeforeEach
    void kmsReleasesKeyOnlyForMatchingContext() {
        byte[] dataKey = new byte[32];
        IntStream.range(0, 32).forEach(i -> dataKey[i] = (byte) i);
        when(kmsClient.decrypt(any(DecryptRequest.class))).thenAnswer(invocation -> {
            DecryptRequest request = invocation.getArgument(0);
            if (!request.ciphertextBlob().asUtf8String().equals("test-edk")
                    || !request.encryptionContext().equals(
                            Map.of("purpose", "bada-recording", "s3_key", KEY))) {
                throw InvalidCiphertextException.builder().message("context mismatch").build();
            }
            return DecryptResponse.builder().plaintext(SdkBytes.fromByteArray(dataKey)).build();
        });
    }

    @Test
    void FastAPI가_잠근_녹음을_연다() {
        assertThat(cipher.isEncrypted(BLOB)).isTrue();
        assertThat(cipher.decrypt(BLOB, KEY)).isEqualTo(WAV);
    }

    @Test
    void 평문_WAV는_잠긴_녹음으로_보지_않는다() {
        assertThat(cipher.isEncrypted(WAV)).isFalse();
        assertThatThrownBy(() -> cipher.decrypt(WAV, KEY)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 다른_키_자리로_옮긴_파일은_열리지_않는다() {
        assertThatThrownBy(() -> cipher.decrypt(BLOB, "recordings/other/b.wav"))
                .isInstanceOf(InvalidCiphertextException.class);
    }

    @Test
    void 변조된_본문은_거부한다() {
        byte[] tampered = BLOB.clone();
        tampered[tampered.length - 1] ^= 0x01;

        assertThatThrownBy(() -> cipher.decrypt(tampered, KEY)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 잘린_파일은_거부한다() {
        byte[] truncated = java.util.Arrays.copyOf(BLOB, 20);

        assertThatThrownBy(() -> cipher.decrypt(truncated, KEY)).isInstanceOf(IllegalArgumentException.class);
    }
}
