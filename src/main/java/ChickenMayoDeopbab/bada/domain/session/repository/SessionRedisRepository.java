package ChickenMayoDeopbab.bada.domain.session.repository;

import ChickenMayoDeopbab.bada.domain.session.exception.SessionStatusCode;
import ChickenMayoDeopbab.bada.domain.session.model.SessionContext;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class SessionRedisRepository {

    private static final String KEY_PREFIX = "session:";
    private static final Duration TTL = Duration.ofHours(1);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(String sessionId, SessionContext context) {
        try {
            String json = objectMapper.writeValueAsString(context);
            redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, json, TTL);
        } catch (JsonProcessingException e) {
            throw ApplicationException.of(SessionStatusCode.REDIS_WRITE_FAILED, e);
        }
    }
}
