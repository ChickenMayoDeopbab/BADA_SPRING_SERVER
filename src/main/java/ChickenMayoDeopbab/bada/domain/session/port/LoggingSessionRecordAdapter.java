package ChickenMayoDeopbab.bada.domain.session.port;

import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;
import ChickenMayoDeopbab.bada.domain.session.model.SessionContext;
import ChickenMayoDeopbab.bada.domain.session.model.TranscriptTurn;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request.TrainingAnalysisRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

// 임시 스텁은 콜백만 검증하고 로그로 남김
// 실제 훈련기록 영구 저장은 준석이가 @Primary 구현으로 교체할거임 ㅇㅇ
@Slf4j
@Component
public class LoggingSessionRecordAdapter implements SessionRecordPort {

    @Override
    public void record(
            String sessionId,
            SessionContext context,
            EndReason reason,
            List<TranscriptTurn> transcript,
            Double silenceTotal,
            Integer shakeCount,
            List<GoodSegment> goodSegments,
            String recordingKey,
            TrainingAnalysisRequest analysis
    ) {
        Long userId = context != null ? context.userId() : null;
        int turns = transcript != null ? transcript.size() : 0;
        int goodSegmentCount = goodSegments != null ? goodSegments.size() : 0;
        log.info("세션 종료 기록 수신 sessionId={} userId={} reason={} turns={} silenceTotal={} shakeCount={} goodSegments={} hasRecording={}",
                sessionId, userId, reason, turns, silenceTotal, shakeCount, goodSegmentCount, recordingKey != null);
    }
}
