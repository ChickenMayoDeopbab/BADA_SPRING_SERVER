package ChickenMayoDeopbab.bada.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PublicEndpointsTest {

    @Test
    void 모든_패턴은_앞_슬래시로_시작한다() {
        Stream.of(PublicEndpoints.POST, PublicEndpoints.GET, PublicEndpoints.ANY)
                .flatMap(Stream::of)
                .forEach(pattern -> assertThat(pattern)
                        .as("공개 경로 패턴 '%s'", pattern)
                        .startsWith("/"));
    }

    @Test
    void 진단_API는_인증_없이_열려_있다() {
        assertThat(isPublic("POST", "/api/diagnosis/submit")).isTrue();
        assertThat(isPublic("GET", "/api/diagnosis/questions")).isTrue();
    }

    @Test
    void 기존_인증_경로는_그대로_열려_있다() {
        assertThat(isPublic("POST", "/api/v1/auth/login")).isTrue();
        assertThat(isPublic("POST", "/api/v1/auth/oauth/token")).isTrue();
        assertThat(isPublic("GET", "/api/v1/auth/google")).isTrue();
        assertThat(isPublic("GET", "/oauth2/authorization/google")).isTrue();
        assertThat(isPublic("GET", "/login/oauth2/code/google")).isTrue();
    }

    @Test
    void 파일_API는_인증을_요구한다() {
        assertThat(isPublic("POST", "/api/v1/file")).isFalse();
        assertThat(isPublic("POST", "/api/v1/file/upload")).isFalse();
    }

    @Test
    void 죽은_경로는_더_이상_열려_있지_않다() {
        assertThat(isPublic("GET", "/images/logo.png")).isFalse();
        assertThat(isPublic("GET", "/css/app.css")).isFalse();
        assertThat(isPublic("GET", "/js/app.js")).isFalse();
        assertThat(isPublic("GET", "/posts/1")).isFalse();
        assertThat(isPublic("GET", "/comments/1")).isFalse();
    }

    @Test
    void 공개_목록에_없는_경로는_막힌다() {
        assertThat(isPublic("GET", "/api/diagnosis/history")).isFalse();
        assertThat(isPublic("DELETE", "/api/v1/auth/withdraw")).isFalse();
    }

    @Test
    void 메서드가_다르면_열리지_않는다() {
        assertThat(isPublic("GET", "/api/v1/auth/login")).isFalse();
        assertThat(isPublic("POST", "/api/diagnosis/questions")).isFalse();
    }

    private boolean isPublic(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setServletPath(uri);
        return PublicEndpoints.matches(request);
    }
}
