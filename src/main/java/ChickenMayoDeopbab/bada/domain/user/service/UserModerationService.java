package ChickenMayoDeopbab.bada.domain.user.service;

import ChickenMayoDeopbab.bada.domain.user.dto.request.UserModerationStatusRequest;
import ChickenMayoDeopbab.bada.domain.user.entity.Role;
import ChickenMayoDeopbab.bada.domain.user.entity.UserStatus;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class UserModerationService {

    private final UsersRepository usersRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserAccessPolicy userAccessPolicy;

    public void updateStatus(Long userId, UserModerationStatusRequest request) {
        Users actor = getUser(request.actorUserId());
        userAccessPolicy.ensureCanAccess(actor);
        if (actor.getRole() != Role.ADMIN) {
            throw ApplicationException.of(UsersStatusCode.MODERATOR_NOT_ALLOWED);
        }

        Users target = getUser(userId);
        if (target.getRole() == Role.ADMIN) {
            throw ApplicationException.of(UsersStatusCode.CANNOT_SANCTION_ADMIN);
        }

        Instant now = Instant.now();
        validate(request, now);
        String reason = request.reason() == null ? null : request.reason().trim();
        target.updateModerationStatus(request.status(), request.suspendedUntil(), now, actor.getUserId(), reason);

        if (request.status() != UserStatus.ACTIVE) {
            redisTemplate.delete(refreshTokenKey(userId));
        }
    }

    private void validate(UserModerationStatusRequest request, Instant now) {
        if (request.status() == UserStatus.ACTIVE) {
            return;
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw ApplicationException.of(UsersStatusCode.INVALID_MODERATION_STATUS);
        }
        if (request.status() == UserStatus.SUSPENDED
                && (request.suspendedUntil() == null || !request.suspendedUntil().isAfter(now))) {
            throw ApplicationException.of(UsersStatusCode.INVALID_MODERATION_STATUS);
        }
        if (request.status() == UserStatus.BANNED && request.suspendedUntil() != null) {
            throw ApplicationException.of(UsersStatusCode.INVALID_MODERATION_STATUS);
        }
    }

    private Users getUser(Long userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> ApplicationException.of(UsersStatusCode.USER_NOT_FOUND));
    }

    private String refreshTokenKey(Long userId) {
        return "refreshToken: " + userId;
    }
}
