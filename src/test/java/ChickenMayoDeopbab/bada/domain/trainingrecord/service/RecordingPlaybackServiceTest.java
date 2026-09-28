package ChickenMayoDeopbab.bada.domain.trainingrecord.service;

import ChickenMayoDeopbab.bada.domain.file.service.FileService;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.trainingrecord.exception.TrainingRecordStatusCode;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Role;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import ChickenMayoDeopbab.bada.global.exception.statuscode.JwtStatusCode;
import ChickenMayoDeopbab.bada.global.jwt.JwtProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecordingPlaybackServiceTest {

    private static final String SERVER = "https://api.bada.test";
    private static final String KEY = "recordings/sess-1/a.wav";
    private static final byte[] LOCKED = "BADAREC1-locked".getBytes();
    private static final byte[] WAV = "RIFF-plain".getBytes();

    private final TrainingRecordRepository trainingRecordRepository = mock(TrainingRecordRepository.class);
    private final FileService fileService = mock(FileService.class);
    private final RecordingCipher recordingCipher = mock(RecordingCipher.class);
    private final JwtProvider jwtProvider = new JwtProvider(
            "test-secret-key-that-is-long-enough-for-hs256", 3_600_000L, 604_800_000L);
    private final RecordingPlaybackService service = new RecordingPlaybackService(
            trainingRecordRepository, fileService, recordingCipher, jwtProvider, SERVER);

    private TrainingRecord record(Long recordId, Long ownerId, String recordingKey) {
        Users owner = mock(Users.class);
        when(owner.getUserId()).thenReturn(ownerId);
        TrainingRecord record = mock(TrainingRecord.class);
        when(record.getRecordId()).thenReturn(recordId);
        when(record.getUser()).thenReturn(owner);
        when(record.getRecordingKey()).thenReturn(recordingKey);
        when(trainingRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        return record;
    }

    private String tokenOf(String url) {
        return url.substring(url.indexOf("token=") + "token=".length());
    }

    private ApplicationException thrownBy(Runnable action) {
        return (ApplicationException) org.assertj.core.api.Assertions.catchThrowable(action::run);
    }

    @Test
    void 재생_링크는_Spring_주소에_재생_토큰을_붙인다() {
        String url = service.playbackUrl(record(1L, 7L, KEY));

        assertThat(url).startsWith(SERVER + "/api/v1/training-records/1/recording?token=");
        JwtProvider.RecordingGrant grant = jwtProvider.parseRecordingToken(tokenOf(url));
        assertThat(grant.userId()).isEqualTo(7L);
        assertThat(grant.recordId()).isEqualTo(1L);
        verify(fileService, never()).generatePresignedUrl(anyString());
    }

    @Test
    void 녹음이_없으면_재생_링크도_없다() {
        assertThat(service.playbackUrl(record(1L, 7L, null))).isNull();
        assertThat(service.playbackUrl(record(2L, 7L, " "))).isNull();
    }

    @Test
    void 잠긴_녹음을_풀어서_준다() {
        String token = tokenOf(service.playbackUrl(record(1L, 7L, KEY)));
        when(fileService.download(KEY)).thenReturn(LOCKED);
        when(recordingCipher.isEncrypted(LOCKED)).thenReturn(true);
        when(recordingCipher.decrypt(LOCKED, KEY)).thenReturn(WAV);

        assertThat(service.open(1L, token)).isEqualTo(WAV);
    }

    @Test
    void 일괄_암호화_전_평문_녹음도_재생한다() {
        String token = tokenOf(service.playbackUrl(record(1L, 7L, KEY)));
        when(fileService.download(KEY)).thenReturn(WAV);

        assertThat(service.open(1L, token)).isEqualTo(WAV);
        verify(recordingCipher, never()).decrypt(WAV, KEY);
    }

    @Test
    void 다른_기록의_토큰으로는_열_수_없다() {
        String token = tokenOf(service.playbackUrl(record(1L, 7L, KEY)));
        record(2L, 7L, "recordings/sess-2/b.wav");

        assertThat(thrownBy(() -> service.open(2L, token)).getStatusCode())
                .isEqualTo(JwtStatusCode.TOKEN_INVALID);
        verifyNoInteractions(fileService);
    }

    @Test
    void 로그인_토큰으로는_열_수_없다() {
        record(1L, 7L, KEY);
        String accessToken = jwtProvider.createAccessToken(7L, Role.USER);

        assertThat(thrownBy(() -> service.open(1L, accessToken)).getStatusCode())
                .isEqualTo(JwtStatusCode.TOKEN_INVALID);
        verifyNoInteractions(fileService);
    }

    @Test
    void 위조된_토큰은_거부한다() {
        record(1L, 7L, KEY);

        assertThatThrownBy(() -> service.open(1L, "not-a-token"))
                .isInstanceOf(ApplicationException.class);
        verifyNoInteractions(fileService);
    }

    @Test
    void 주인이_바뀐_기록은_찾을_수_없다() {
        String token = jwtProvider.createRecordingToken(99L, 1L);
        record(1L, 7L, KEY);

        assertThat(thrownBy(() -> service.open(1L, token)).getStatusCode())
                .isEqualTo(TrainingRecordStatusCode.RECORD_NOT_FOUND);
        verifyNoInteractions(fileService);
    }

    @Test
    void 녹음이_없는_기록은_404() {
        String token = jwtProvider.createRecordingToken(7L, 1L);
        record(1L, 7L, null);

        assertThat(thrownBy(() -> service.open(1L, token)).getStatusCode())
                .isEqualTo(TrainingRecordStatusCode.RECORDING_NOT_FOUND);
    }
}
