package ChickenMayoDeopbab.bada.domain.auth.service;

import ChickenMayoDeopbab.bada.domain.auth.exception.AuthStatusCode;
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
    private final JavaMailSender javaMailSender;
    private final RedisConfig redisConfig;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String serviceName;

    public void sendEmail(String setFrom, String toMail, String title, String content, int authNum) {
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
        valueOperations.set(toMail, Integer.toString(authNum), 5, TimeUnit.MINUTES);
    }

    public String joinEmail(String email) {
        Random random = new Random();
        int authNum = 100000 + random.nextInt(900000);

        String title = "회원가입을 위한 인증코드입니다.";

        Context context = new Context();
        context.setVariable("authNum", authNum);

        String content = templateEngine.process("EmailAuth", context);
        sendEmail(serviceName, email, title, content, authNum);

        return Integer.toString(authNum);
    }

    public ApiResponse<Boolean> checkEmail(String email, String authNum) {
        ValueOperations<String, String> valueOperations = redisConfig.redisTemplate().opsForValue();
        String code = valueOperations.get(email);

        if (Objects.equals(code, authNum)) {
            redisConfig.redisTemplate().delete(email);
            return ApiResponse.ok(Boolean.TRUE, "이메일 인증에 성공했습니다.");
        }
        throw new ApplicationException(AuthStatusCode.INVALID_VERIFICATION_CODE);
    }
}
