package ChickenMayoDeopbab.bada.domain.user.port;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class FastApiUserDataCleanupAdapter implements UserDataCleanupPort {

    @Value("${app.ai.base-url}")
    private String aiBaseUrl;

    @Value("${app.internal.secret}")
    private String internalSecret;

    @Override
    public void deleteByUserId(Long userId) {
        RestClient.create()
                .delete()
                .uri(aiBaseUrl + "/internal/v1/users/{userId}/data", userId)
                .header("X-Internal-Secret", internalSecret)
                .retrieve()
                .toBodilessEntity();
    }
}
