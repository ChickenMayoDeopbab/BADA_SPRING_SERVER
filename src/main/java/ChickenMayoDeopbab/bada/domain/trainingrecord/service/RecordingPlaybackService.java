package ChickenMayoDeopbab.bada.domain.trainingrecord.service;

import ChickenMayoDeopbab.bada.domain.file.service.FileService;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.trainingrecord.exception.TrainingRecordStatusCode;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import ChickenMayoDeopbab.bada.global.exception.statuscode.JwtStatusCode;
import ChickenMayoDeopbab.bada.global.jwt.JwtProvider;
import ChickenMayoDeopbab.bada.global.jwt.JwtProvider.RecordingGrant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 녹음 원본은 S3 에 잠긴 채 있어 S3 presigned URL 로는 재생할 수 없다.
 * 대신 10분짜리 재생 토큰을 붙인 Spring 주소를 내주고, 요청이 오면 풀어서 흘려보낸다.
 */
@Service
public class RecordingPlaybackService {

    private final TrainingRecordRepository trainingRecordRepository;
    private final FileService fileService;
    private final RecordingCipher recordingCipher;
    private final JwtProvider jwtProvider;
    private final String serverUrl;

    public RecordingPlaybackService(
            TrainingRecordRepository trainingRecordRepository,
            FileService fileService,
            RecordingCipher recordingCipher,
            JwtProvider jwtProvider,
            @Value("${app.server-url}") String serverUrl
    ) {
        this.trainingRecordRepository = trainingRecordRepository;
        this.fileService = fileService;
        this.recordingCipher = recordingCipher;
        this.jwtProvider = jwtProvider;
        this.serverUrl = serverUrl;
    }

    public String playbackUrl(TrainingRecord record) {
        if (!hasRecording(record)) {
            return null;
        }
        String token = jwtProvider.createRecordingToken(record.getUser().getUserId(), record.getRecordId());
        return serverUrl + "/api/v1/training-records/" + record.getRecordId() + "/recording?token=" + token;
    }

    public byte[] open(Long recordId, String token) {
        RecordingGrant grant = jwtProvider.parseRecordingToken(token);
        if (!grant.recordId().equals(recordId)) {
            throw ApplicationException.of(JwtStatusCode.TOKEN_INVALID);
        }
        TrainingRecord record = trainingRecordRepository.findById(recordId)
                .filter(found -> grant.userId().equals(found.getUser().getUserId()))
                .orElseThrow(() -> ApplicationException.of(TrainingRecordStatusCode.RECORD_NOT_FOUND));
        if (!hasRecording(record)) {
            throw ApplicationException.of(TrainingRecordStatusCode.RECORDING_NOT_FOUND);
        }

        String key = record.getRecordingKey();
        byte[] body = fileService.download(key);
        // 일괄 암호화 전에 올라간 평문 녹음도 그대로 재생한다.
        return recordingCipher.isEncrypted(body) ? recordingCipher.decrypt(body, key) : body;
    }

    private boolean hasRecording(TrainingRecord record) {
        String key = record.getRecordingKey();
        return key != null && !key.isBlank();
    }
}
