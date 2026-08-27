package ChickenMayoDeopbab.bada.domain.auth.service;

import ChickenMayoDeopbab.bada.domain.auth.exception.AuthStatusCode;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthRedirectUriResolverTest {

    private static final String APP_URI = "bada://auth/callback";
    private static final String WEB_URI = "https://bada.chickenmayo.kr/oauth/callback";

    private final OAuthRedirectUriResolver resolver =
            new OAuthRedirectUriResolver(APP_URI, WEB_URI + ", http://localhost:3000/oauth/callback");

    @Test
    void redirectUri를_생략하면_앱_딥링크로_복귀한다() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        resolver.remember(new MockHttpServletRequest(), response, null);

        assertThat(consumeWith(response)).isEqualTo(APP_URI);
    }

    @Test
    void 허용된_웹_주소는_그대로_복귀_대상이_된다() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        resolver.remember(new MockHttpServletRequest(), response, WEB_URI);

        assertThat(consumeWith(response)).isEqualTo(WEB_URI);
    }

    @Test
    void 허용목록에_없는_주소는_거부한다() {
        assertThatThrownBy(() -> resolver.remember(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                "https://evil.example.com/steal"))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("statusCode", AuthStatusCode.UNSUPPORTED_REDIRECT_URI);
    }

    @Test
    void 위조된_쿠키는_무시하고_기본값으로_되돌린다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("oauthRedirectUri", "https%3A%2F%2Fevil.example.com"));

        assertThat(resolver.consume(request, new MockHttpServletResponse())).isEqualTo(APP_URI);
    }

    @Test
    void 쿠키가_없으면_기본값으로_되돌린다() {
        assertThat(resolver.consume(new MockHttpServletRequest(), new MockHttpServletResponse()))
                .isEqualTo(APP_URI);
    }

    @Test
    void 꺼내고_나면_쿠키를_즉시_만료시킨다() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        resolver.consume(new MockHttpServletRequest(), response);

        assertThat(response.getCookie("oauthRedirectUri").getMaxAge()).isZero();
    }

    @Test
    void 쿠키는_HttpOnly와_SameSite_Lax로_내려간다() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        resolver.remember(new MockHttpServletRequest(), response, WEB_URI);

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .contains("HttpOnly")
                .contains("SameSite=Lax");
    }

    @Test
    void https_요청일_때만_Secure_플래그를_붙인다() {
        MockHttpServletRequest secure = new MockHttpServletRequest();
        secure.setSecure(true);
        MockHttpServletResponse secureResponse = new MockHttpServletResponse();
        resolver.remember(secure, secureResponse, WEB_URI);

        MockHttpServletResponse plainResponse = new MockHttpServletResponse();
        resolver.remember(new MockHttpServletRequest(), plainResponse, WEB_URI);

        assertThat(secureResponse.getHeader(HttpHeaders.SET_COOKIE)).contains("Secure");
        assertThat(plainResponse.getHeader(HttpHeaders.SET_COOKIE)).doesNotContain("Secure");
    }

    private String consumeWith(MockHttpServletResponse response) {
        Cookie issued = response.getCookie("oauthRedirectUri");
        assertThat(issued).isNotNull();

        MockHttpServletRequest next = new MockHttpServletRequest();
        next.setCookies(new Cookie(issued.getName(), issued.getValue()));

        return resolver.consume(next, new MockHttpServletResponse());
    }
}
