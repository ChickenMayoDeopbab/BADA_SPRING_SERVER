package ChickenMayoDeopbab.bada.domain.auth.service;

import ChickenMayoDeopbab.bada.domain.auth.enums.AuthEmailType;
import ChickenMayoDeopbab.bada.domain.auth.exception.AuthStatusCode;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.config.RedisConfig;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {
    public static final String VERIFIED = "ACCESS";

    private final JavaMailSender javaMailSender;
    private final RedisConfig redisConfig;
    private final TemplateEngine templateEngine;
    private final UsersRepository usersRepository;

    @Value("${spring.mail.username}")
    private String serviceName;

    public void sendEmail(String setFrom, String toMail, String title, String content, String redisKey, int authNum) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(setFrom);
            helper.setTo(toMail);
            helper.setSubject(title);
            helper.setText(content, true);

            javaMailSender.send(mimeMessage);

        } catch (Exception e) {
            log.error("이메일 전송에 실패했습니다.", e);
            return;
        }

        ValueOperations<String, String> valueOperations = redisConfig.redisTemplate().opsForValue();
        valueOperations.set(redisKey, Integer.toString(authNum), 5, TimeUnit.MINUTES);
    }

    public String joinEmail(String email, AuthEmailType type) {
        validateEmailTarget(email, type);

        Random random = new Random();
        int authNum = 100000 + random.nextInt(900000);

        Context context = new Context();
        context.setVariable("authNum", authNum);

        String content = templateEngine.process("EmailAuth", context);
        sendEmail(serviceName, email, type.getEmailTitle(), content, type.redisKey(email), authNum);

        return Integer.toString(authNum);
    }

    public ApiResponse<Boolean> checkEmail(String email, String authNum, AuthEmailType type) {
        String redisKey = type.redisKey(email);
        ValueOperations<String, String> valueOperations = redisConfig.redisTemplate().opsForValue();
        String code = valueOperations.get(redisKey);

        if (Objects.equals(code, authNum)) {
            valueOperations.set(redisKey, VERIFIED, 5, TimeUnit.MINUTES);
            return ApiResponse.ok(Boolean.TRUE, "이메일 인증에 성공했습니다.");
        }
        throw new ApplicationException(AuthStatusCode.INVALID_VERIFICATION_CODE);
    }

    private void validateEmailTarget(String email, AuthEmailType type) {
        boolean exists = usersRepository.existsByEmail(email);

        if (type == AuthEmailType.SIGNUP && exists) {
            throw new ApplicationException(UsersStatusCode.DUPLICATE_EMAIL);
        }
    }
}
