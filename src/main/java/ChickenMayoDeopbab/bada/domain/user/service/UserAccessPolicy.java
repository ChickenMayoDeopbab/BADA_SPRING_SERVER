package ChickenMayoDeopbab.bada.domain.user.service;

import ChickenMayoDeopbab.bada.domain.user.entity.UserStatus;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class UserAccessPolicy {

    public void ensureCanAccess(Users user) {
        user.activateIfSuspensionExpired(Instant.now());
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw ApplicationException.of(UsersStatusCode.USER_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.BANNED) {
            throw ApplicationException.of(UsersStatusCode.USER_BANNED);
        }
    }
}
