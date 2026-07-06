package ChickenMayoDeopbab.bada.domain.session.model;

import java.util.List;

public record ScenarioContext(String title, String aiRole, String aiPrompt, List<ScriptTurn> script, String ttsVoiceId) {
}
