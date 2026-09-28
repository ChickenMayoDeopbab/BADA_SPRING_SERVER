package ChickenMayoDeopbab.bada.domain.trainingrecord.port;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class FastApiTrainingDataCleanupAdapter implements TrainingDataCleanupPort {

    @Value("${app.ai.base-url}")
    private String aiBaseUrl;

    @Value("${app.internal.secret}")
    private String internalSecret;

    @Override
    public void deleteBySessionId(String sessionId) {
        RestClient.create()
                .delete()
                .uri(aiBaseUrl + "/internal/v1/training-data/{sessionId}", sessionId)
                .header("X-Internal-Secret", internalSecret)
                .retrieve()
                .toBodilessEntity();
    }
}
