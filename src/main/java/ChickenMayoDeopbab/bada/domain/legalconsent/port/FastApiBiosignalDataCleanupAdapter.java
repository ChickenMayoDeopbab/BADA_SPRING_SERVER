package ChickenMayoDeopbab.bada.domain.legalconsent.port;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class FastApiBiosignalDataCleanupAdapter implements BiosignalDataCleanupPort {

    @Value("${app.ai.base-url}")
    private String aiBaseUrl;

    @Value("${app.internal.secret}")
    private String internalSecret;

    @Override
    public void deleteByUserId(Long userId) {
        RestClient.create()
                .delete()
                .uri(aiBaseUrl + "/internal/v1/users/{userId}/biosignals", userId)
                .header("X-Internal-Secret", internalSecret)
                .retrieve()
                .toBodilessEntity();
    }
}
