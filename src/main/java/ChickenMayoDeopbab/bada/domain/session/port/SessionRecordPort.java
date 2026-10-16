package ChickenMayoDeopbab.bada.domain.session.port;

import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;
import ChickenMayoDeopbab.bada.domain.session.model.SessionContext;
import ChickenMayoDeopbab.bada.domain.session.model.TranscriptTurn;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request.TrainingAnalysisRequest;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingSpeechMetrics;

import java.util.List;

// 종료된 세션의 훈련기록 저장 이어주는거
// 실제 영구 저장 구현은 준석이가 @Primary 구현으로 채울거임 ㅇㅇ
public interface SessionRecordPort {
    // 기존 내부 호출자는 음성 요약 없이도 기록을 저장할 수 있다.
    default void record(String sessionId, SessionContext context, EndReason reason,
                        List<TranscriptTurn> transcript, Double silenceTotal, Integer shakeCount,
                        List<GoodSegment> goodSegments, String recordingKey, TrainingAnalysisRequest analysis) {
        record(sessionId, context, reason, transcript, silenceTotal, shakeCount,
                goodSegments, recordingKey, analysis, null);
    }

    void record(
            String sessionId,
            SessionContext context,
            EndReason reason,
            List<TranscriptTurn> transcript,
            Double silenceTotal,
            Integer shakeCount,
            List<GoodSegment> goodSegments,
            String recordingKey,
            TrainingAnalysisRequest analysis,
            TrainingSpeechMetrics speechMetrics
    );
}
