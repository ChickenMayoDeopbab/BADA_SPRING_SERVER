package ChickenMayoDeopbab.bada.domain.notification.port;

import ChickenMayoDeopbab.bada.domain.notification.model.PushMessage;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;

public interface PushMessageSender {

    void send(Users recipient, PushMessage message);
}
