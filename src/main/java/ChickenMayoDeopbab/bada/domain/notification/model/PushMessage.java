package ChickenMayoDeopbab.bada.domain.notification.model;

import java.util.Map;

public record PushMessage(
        String title,
        String body,
        Map<String, String> data
) {

    public PushMessage {
        data = Map.copyOf(data);
    }
}
