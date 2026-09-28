package ChickenMayoDeopbab.bada.domain.trainingrecord.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Map;

/**
 * FastAPI 가 KMS 봉투 암호화로 잠근 녹음 원본을 연다.
 * 형식은 FastAPI app/services/recording_crypto.py 와 같아야 한다.
 * <pre>
 * MAGIC(8) | edk_len(2, big-endian) | edk | nonce(12) | ciphertext + tag(16)
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class RecordingCipher {

    private static final byte[] MAGIC = "BADAREC1".getBytes(StandardCharsets.US_ASCII);
    private static final int LENGTH_BYTES = 2;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final KmsClient kmsClient;

    public boolean isEncrypted(byte[] blob) {
        return blob.length >= MAGIC.length
                && Arrays.equals(blob, 0, MAGIC.length, MAGIC, 0, MAGIC.length);
    }

    public byte[] decrypt(byte[] blob, String s3Key) {
        if (!isEncrypted(blob) || blob.length < MAGIC.length + LENGTH_BYTES) {
            throw new IllegalArgumentException("암호화된 녹음이 아님");
        }
        int edkStart = MAGIC.length + LENGTH_BYTES;
        int edkLength = ((blob[MAGIC.length] & 0xff) << 8) | (blob[MAGIC.length + 1] & 0xff);
        int headerEnd = edkStart + edkLength;
        int bodyStart = headerEnd + NONCE_BYTES;
        if (blob.length < bodyStart + TAG_BITS / 8) {
            throw new IllegalArgumentException("녹음 앞머리가 잘림");
        }

        byte[] dataKey = kmsClient.decrypt(DecryptRequest.builder()
                        .ciphertextBlob(SdkBytes.fromByteArray(Arrays.copyOfRange(blob, edkStart, headerEnd)))
                        .encryptionContext(Map.of("purpose", "bada-recording", "s3_key", s3Key))
                        .build())
                .plaintext()
                .asByteArray();
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(dataKey, "AES"),
                    new GCMParameterSpec(TAG_BITS, blob, headerEnd, NONCE_BYTES)
            );
            cipher.updateAAD(blob, 0, headerEnd);
            return cipher.doFinal(blob, bodyStart, blob.length - bodyStart);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("녹음 복호화 실패", e);
        } finally {
            Arrays.fill(dataKey, (byte) 0);
        }
    }
}
