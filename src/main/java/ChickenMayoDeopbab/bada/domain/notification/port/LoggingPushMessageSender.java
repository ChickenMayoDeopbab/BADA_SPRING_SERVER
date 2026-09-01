package ChickenMayoDeopbab.bada.domain.notification.port;

import ChickenMayoDeopbab.bada.domain.notification.model.PushMessage;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.firebase.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class LoggingPushMessageSender implements PushMessageSender {

    @Override
    public void send(Users recipient, PushMessage message) {
        log.info(
                "푸시 알림 이벤트 생성 userId={} notificationType={} data={}",
                recipient.getUserId(),
                message.data().get("notificationType"),
                message.data()
        );
    }
}
