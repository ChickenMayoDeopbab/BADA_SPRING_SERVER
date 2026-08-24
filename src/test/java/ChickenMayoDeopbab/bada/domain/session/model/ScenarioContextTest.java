package ChickenMayoDeopbab.bada.domain.session.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioContextTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsAiPromptFromFastApiContextResponse() throws Exception {
        String json = """
                {
                  "title": "병원 예약 변경",
                  "ttsVoiceId": "v-abc",
                  "aiRole": "병원 접수 직원",
                  "aiPrompt": "You are a hospital receptionist handling appointment changes.",
                  "script": [
                    {
                      "step": 1,
                      "aiGoal": "병원 이름을 밝히고 용건을 확인한다",
                      "hint": "예약 변경을 말하세요"
                    }
                  ]
                }
                """;

        ScenarioContext context = objectMapper.readValue(json, ScenarioContext.class);

        assertThat(context.title()).isEqualTo("병원 예약 변경");
        assertThat(context.aiRole()).isEqualTo("병원 접수 직원");
        assertThat(context.aiPrompt()).isEqualTo("You are a hospital receptionist handling appointment changes.");
        assertThat(context.script()).hasSize(1);
        assertThat(context.script().get(0).aiGoal()).isEqualTo("병원 이름을 밝히고 용건을 확인한다");
        assertThat(context.ttsVoiceId()).isEqualTo("v-abc");
    }

    @Test
    void readsNullTtsVoiceIdWhenFieldAbsent() throws Exception {
        String json = """
                {
                  "title": "병원 예약 변경",
                  "aiRole": "병원 접수 직원",
                  "aiPrompt": "You are a hospital receptionist handling appointment changes.",
                  "script": [
                    {
                      "step": 1,
                      "aiGoal": "병원 이름을 밝히고 용건을 확인한다",
                      "hint": "예약 변경을 말하세요"
                    }
                  ]
                }
                """;

        ScenarioContext context = objectMapper.readValue(json, ScenarioContext.class);

        assertThat(context.ttsVoiceId()).isNull();
    }

    @Test
    void writesAiPromptToRedisSessionJson() throws Exception {
        ScenarioContext context = new ScenarioContext(
                "병원 예약 변경",
                "병원 접수 직원",
                "You are a hospital receptionist handling appointment changes.",
                java.util.List.of(new ScriptTurn(1, "병원 이름을 밝히고 용건을 확인한다", "예약 변경을 말하세요")),
                "v-abc",
                null
        );

        String json = objectMapper.writeValueAsString(context);

        assertThat(json).contains("\"aiPrompt\":\"You are a hospital receptionist handling appointment changes.\"");
        assertThat(json).contains("\"ttsVoiceId\":\"v-abc\"");
    }
}
