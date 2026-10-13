package ChickenMayoDeopbab.bada.domain.file.service;

import ChickenMayoDeopbab.bada.domain.file.exception.FileStatusCode;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class RingtoneFileValidatorTest {

    @Test
    void acceptsPcmWaveAndNormalizesUntrustedMime() throws IOException {
        assertThat(RingtoneFileValidator.validate(file("ring.WAV", RingtoneAudioFixtures.wave())))
                .isEqualTo("audio/wav");
    }

    @Test
    void acceptsMpegLayerThreeFrames() {
        assertThat(RingtoneFileValidator.validate(file("ring.MP3", RingtoneAudioFixtures.mp3())))
                .isEqualTo("audio/mpeg");
    }

    @Test
    void acceptsId3MetadataBeforeMp3Frames() {
        byte[] frames = RingtoneAudioFixtures.mp3();
        byte[] tagged = new byte[10 + frames.length];
        tagged[0] = 'I'; tagged[1] = 'D'; tagged[2] = '3'; tagged[3] = 4;
        System.arraycopy(frames, 0, tagged, 10, frames.length);

        assertThat(RingtoneFileValidator.validate(file("ring.mp3", tagged))).isEqualTo("audio/mpeg");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ring.exe", "ring.m4a", "ring", "ring.mp3"})
    void rejectsUnsupportedOrMismatchedExtensions(String filename) throws IOException {
        assertInvalid(file(filename, RingtoneAudioFixtures.wave()));
    }

    @Test
    void rejectsSpoofedAudioMime() {
        assertInvalid(new MockMultipartFile("file", "ring.mp3", "audio/mpeg", "not audio".getBytes()));
    }

    @Test
    void rejectsTruncatedWaveData() throws IOException {
        byte[] wave = RingtoneAudioFixtures.wave();
        assertInvalid(file("ring.wav", Arrays.copyOf(wave, wave.length - 2)));
    }

    @Test
    void rejectsTruncatedMp3Frame() {
        byte[] frames = RingtoneAudioFixtures.mp3();
        assertInvalid(file("ring.mp3", Arrays.copyOf(frames, frames.length - 1)));
    }

    @Test
    void rejectsMetadataWithoutMp3Audio() {
        assertInvalid(file("ring.mp3", new byte[]{'I', 'D', '3', 4, 0, 0, 0, 0, 0, 0}));
    }

    @Test
    void rejectsInvalidId3SizeWithoutReadingBeyondFile() {
        assertInvalid(file("ring.mp3", new byte[]{'I', 'D', '3', 4, 0, 0, (byte) 0xff, 0, 0, 0}));
    }

    @Test
    void rejectsOversizedFileBeforeReadingItsBytes() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(RingtoneFileValidator.MAX_FILE_SIZE + 1);

        assertThatThrownBy(() -> RingtoneFileValidator.validate(file))
                .isInstanceOf(ApplicationException.class).extracting("statusCode")
                .isEqualTo(FileStatusCode.RINGTONE_TOO_LARGE);
        verify(file, never()).getBytes();
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> RingtoneFileValidator.validate(file("ring.wav", new byte[0])))
                .isInstanceOf(ApplicationException.class).extracting("statusCode")
                .isEqualTo(FileStatusCode.EMPTY_FILE);
    }

    private MockMultipartFile file(String filename, byte[] content) {
        return new MockMultipartFile("file", filename, "application/octet-stream", content);
    }

    private void assertInvalid(MultipartFile file) {
        assertThatThrownBy(() -> RingtoneFileValidator.validate(file))
                .isInstanceOf(ApplicationException.class).extracting("statusCode")
                .isEqualTo(FileStatusCode.INVALID_RINGTONE_TYPE);
    }
}
