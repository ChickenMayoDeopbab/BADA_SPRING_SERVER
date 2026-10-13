package ChickenMayoDeopbab.bada.domain.file.service;

import ChickenMayoDeopbab.bada.domain.file.exception.FileStatusCode;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

public final class RingtoneFileValidator {

    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int[] MPEG1_BITRATES = {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320};
    private static final int[] MPEG2_BITRATES = {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160};
    private static final int[] SAMPLE_RATES = {44100, 48000, 32000};

    private RingtoneFileValidator() {
    }

    public static String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApplicationException.of(FileStatusCode.EMPTY_FILE);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw ApplicationException.of(FileStatusCode.RINGTONE_TOO_LARGE);
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank() || filename.length() > 255) {
            throw ApplicationException.of(FileStatusCode.INVALID_RINGTONE_TYPE);
        }
        try {
            byte[] audio = file.getBytes();
            String name = filename.toLowerCase(Locale.ROOT);
            // 클라이언트의 확장자와 MIME만 신뢰하지 않고 실제 컨테이너/프레임을 확인한다.
            if (name.endsWith(".wav") && isPcmWave(audio)) {
                return "audio/wav";
            }
            if (name.endsWith(".mp3") && isMp3(audio)) {
                return "audio/mpeg";
            }
        } catch (IOException exception) {
            throw ApplicationException.of(FileStatusCode.FILE_UPLOAD_FAILED, exception);
        }
        throw ApplicationException.of(FileStatusCode.INVALID_RINGTONE_TYPE);
    }

    private static boolean isPcmWave(byte[] audio) {
        if (audio.length < 44 || !matches(audio, 0, "RIFF") || !matches(audio, 8, "WAVE")) {
            return false;
        }
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audio))) {
            AudioFormat format = stream.getFormat();
            if ((!AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())
                    && !AudioFormat.Encoding.PCM_UNSIGNED.equals(format.getEncoding()))
                    || format.getChannels() < 1 || format.getChannels() > 2
                    || format.getFrameSize() <= 0 || stream.getFrameLength() <= 0) {
                return false;
            }
            long expected = stream.getFrameLength() * format.getFrameSize();
            return expected <= audio.length && stream.transferTo(java.io.OutputStream.nullOutputStream()) == expected;
        } catch (IOException | UnsupportedAudioFileException exception) {
            return false;
        }
    }

    private static boolean isMp3(byte[] audio) {
        int offset = 0;
        if (matches(audio, 0, "ID3")) {
            if (audio.length < 10 || audio[3] < 2 || audio[3] > 4) {
                return false;
            }
            int tagSize = 0;
            for (int index = 6; index < 10; index++) {
                if ((audio[index] & 0x80) != 0) {
                    return false;
                }
                tagSize = (tagSize << 7) | audio[index];
            }
            offset = 10 + tagSize;
            if (audio[3] == 4 && (audio[5] & 0x10) != 0) {
                offset += 10;
            }
        }
        int frameCount = 0;
        while (offset + 4 <= audio.length) {
            if (audio.length - offset == 128 && matches(audio, offset, "TAG")) {
                offset += 128;
                break;
            }
            int header = ((audio[offset] & 0xff) << 24) | ((audio[offset + 1] & 0xff) << 16)
                    | ((audio[offset + 2] & 0xff) << 8) | (audio[offset + 3] & 0xff);
            int version = (header >>> 19) & 3;
            int layer = (header >>> 17) & 3;
            int bitrateIndex = (header >>> 12) & 15;
            int sampleRateIndex = (header >>> 10) & 3;
            if ((header & 0xffe00000) != 0xffe00000 || version == 1 || layer != 1
                    || bitrateIndex == 0 || bitrateIndex == 15 || sampleRateIndex == 3) {
                return false;
            }
            int sampleRate = SAMPLE_RATES[sampleRateIndex] / (version == 3 ? 1 : version == 2 ? 2 : 4);
            int bitrate = (version == 3 ? MPEG1_BITRATES : MPEG2_BITRATES)[bitrateIndex] * 1000;
            int length = (version == 3 ? 144 : 72) * bitrate / sampleRate + ((header >>> 9) & 1);
            if (offset + length > audio.length) {
                return false;
            }
            offset += length;
            frameCount++;
        }
        return frameCount >= 2 && offset == audio.length;
    }

    private static boolean matches(byte[] data, int offset, String text) {
        if (offset + text.length() > data.length) {
            return false;
        }
        for (int index = 0; index < text.length(); index++) {
            if (data[offset + index] != text.charAt(index)) {
                return false;
            }
        }
        return true;
    }
}
