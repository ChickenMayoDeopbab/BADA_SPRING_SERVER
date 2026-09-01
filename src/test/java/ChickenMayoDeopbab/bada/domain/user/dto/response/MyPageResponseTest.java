package ChickenMayoDeopbab.bada.domain.user.dto.response;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisResult;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 애플 이메일 가리기와 네이버 선택 동의 거부를 받아들이려고 Users.email의 NOT NULL을 풀었다.
 * email을 꺼내 쓰는 곳은 마이페이지 응답뿐이므로 여기서 null이 안전한지 확인한다.
 */
class MyPageResponseTest {

    private DiagnosisResult diagnosisResult() {
        return DiagnosisResult.builder()
                .level(CallPhobiaLevel.LEVEL_3)
                .score(42.5)
                .updatedAt(LocalDateTime.of(2026, 9, 1, 12, 0))
                .build();
    }

    @Test
    @DisplayName("이메일이 없는 소셜 회원도 마이페이지 응답을 만들 수 있다")
    void buildsWithoutEmail() {
        Users user = Users.builder().username("USER_abc12345").email(null).build();

        assertThatCode(() -> MyPageResponse.of(user, diagnosisResult(), 3, 7)).doesNotThrowAnyException();

        MyPageResponse response = MyPageResponse.of(user, diagnosisResult(), 3, 7);
        assertThat(response.email()).isNull();
        assertThat(response.username()).isEqualTo("USER_abc12345");
        assertThat(response.trainCount()).isEqualTo(3);
        assertThat(response.attendance()).isEqualTo(7);
    }

    @Test
    @DisplayName("이메일이 있으면 그대로 실어 보낸다")
    void keepsEmailWhenPresent() {
        Users user = Users.builder().username("USER_abc12345").email("a@b.com").build();

        assertThat(MyPageResponse.of(user, diagnosisResult(), 0, 0).email()).isEqualTo("a@b.com");
    }
}
