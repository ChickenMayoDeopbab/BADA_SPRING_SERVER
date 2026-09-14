package ChickenMayoDeopbab.bada.domain.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthEmailType {
    SIGNUP("회원가입을 위한 인증코드입니다."),
    FIND_ID("아이디 찾기를 위한 인증코드입니다."),
    RESET_PASSWORD("비밀번호 재설정을 위한 인증코드입니다."),

    ;

    private final String emailTitle;

    public String redisKey(String email) {
        return "EMAIL_AUTH:" + name() + ":" + email;
    }
}
