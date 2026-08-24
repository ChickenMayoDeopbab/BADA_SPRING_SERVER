package ChickenMayoDeopbab.bada.domain.session.model;

import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionContextTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void writesScriptLevelToRedisSessionJson() throws Exception {
        SessionContext context = new SessionContext(
                7L,
                3L,
                SessionType.SCENARIO,
                AiPersonality.NORMAL,
                180,
                null,
                new ScenarioContext("음식점 예약", "사장님", "prompt", List.of(), null, null),
                2,
                "medium"
        );

        String json = objectMapper.writeValueAsString(context);

        assertThat(json).contains("\"scriptLevel\":2");
    }

    @Test
    void readsLegacySessionJsonWithoutScriptLevel() throws Exception {
        String json = """
                {
                  "userId": 7,
                  "scenarioId": 3,
                  "type": "SCENARIO",
                  "aiPersonality": "NORMAL",
                  "maxDurationSeconds": 180,
                  "startedAt": null,
                  "scenario": null
                }
                """;

        SessionContext context = objectMapper.readValue(json, SessionContext.class);

        assertThat(context.scriptLevel()).isNull();
        assertThat(context.userId()).isEqualTo(7L);
    }
}
