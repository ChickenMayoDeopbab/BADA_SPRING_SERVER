package ChickenMayoDeopbab.bada.domain.session.port;

import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;
import ChickenMayoDeopbab.bada.domain.session.model.SessionContext;
import ChickenMayoDeopbab.bada.domain.session.model.TranscriptTurn;

import java.util.List;

// 종료된 세션의 훈련기록 저장 이어주는거
// 실제 영구 저장 구현은 준석이가 @Primary 구현으로 채울거임 ㅇㅇ
public interface SessionRecordPort {
    void record(
            String sessionId,
            SessionContext context,
            EndReason reason,
            List<TranscriptTurn> transcript,
            Double silenceTotal,
            Integer shakeCount,
            List<GoodSegment> goodSegments,
            String recordingKey
    );
}
