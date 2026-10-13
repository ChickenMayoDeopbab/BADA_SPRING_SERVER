package ChickenMayoDeopbab.bada.domain.file.service;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class RingtoneAudioFixtures {

    private RingtoneAudioFixtures() {
    }

    public static byte[] wave() throws IOException {
        return wave(160);
    }

    public static byte[] wave(int pcmByteCount) throws IOException {
        byte[] pcm = new byte[pcmByteCount];
        AudioFormat format = new AudioFormat(8000, 16, 1, true, false);
        try (AudioInputStream audio = new AudioInputStream(new ByteArrayInputStream(pcm), format, pcmByteCount / format.getFrameSize());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            AudioSystem.write(audio, AudioFileFormat.Type.WAVE, output);
            return output.toByteArray();
        }
    }

    public static byte[] mp3() {
        byte[] frames = new byte[834];
        for (int offset : new int[]{0, 417}) {
            frames[offset] = (byte) 0xff;
            frames[offset + 1] = (byte) 0xfb;
            frames[offset + 2] = (byte) 0x90;
        }
        return frames;
    }
}
