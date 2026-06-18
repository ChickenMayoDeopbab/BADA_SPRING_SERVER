package ChickenMayoDeopbab.bada.domain.session.model;

// 종료 콜백으로 넘어오는 대화 하나 (role: user/assistant)
public record TranscriptTurn(String role, String text) {
}
