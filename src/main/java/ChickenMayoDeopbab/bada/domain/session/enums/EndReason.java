package ChickenMayoDeopbab.bada.domain.session.enums;

// FastAPI가 종료 콜백으로 보내는 종료 사유
public enum EndReason {
    SCENARIO_DONE,  // 시나리오 마지막 step 종료
    USER_END,       // 클라가 end 프레임 전송
    TIMEOUT,        // 최대 시간 초과
    CRISIS,         // 이상한거 대화하면 종료
    END_CALL,       // LLM이 끝났다고 신호
    NO_AUDIO,       // STT가 오디오 미수신으로 정상 종료
    ERROR           // 서버 에러로 종료
}
