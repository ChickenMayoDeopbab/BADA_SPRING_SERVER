package ChickenMayoDeopbab.bada.domain.session.port;

import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.session.exception.SessionStatusCode;
import ChickenMayoDeopbab.bada.domain.session.model.ScenarioContext;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Primary
@Component
@RequiredArgsConstructor
public class FastApiScenarioAdapter implements ScenarioPort {

    @Value("${app.ai.base-url}")     private String aiBaseUrl;
    @Value("${app.internal.secret}") private String internalSecret;

    @Override
    public ScenarioContext fetch(Long scenarioId, SessionType type) {
        try {
            return RestClient.create()
                    .get()
                    .uri(aiBaseUrl + "/internal/v1/scenarios/{id}/context", scenarioId)
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .body(ScenarioContext.class);
        } catch (Exception e) {
            throw ApplicationException.of(SessionStatusCode.SCENARIO_NOT_FOUND, e);
        }
    }
}