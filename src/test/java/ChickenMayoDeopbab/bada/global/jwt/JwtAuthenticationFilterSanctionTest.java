package ChickenMayoDeopbab.bada.global.jwt;

import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterSanctionTest {

    @Test
    void rejectsExistingAccessTokenWhenUserIsBanned() throws Exception {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        MemberDetailsService memberDetailsService = mock(MemberDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                new ObjectMapper().findAndRegisterModules(), jwtProvider, memberDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(jwtProvider.resolveToken(request)).thenReturn("access-token");
        when(jwtProvider.getTypeFromToken("access-token")).thenReturn(Optional.of("ACCESS"));
        when(jwtProvider.getUserIdFromToken("access-token")).thenReturn(7L);
        doThrow(ApplicationException.of(UsersStatusCode.USER_BANNED))
                .when(memberDetailsService).loadUserById(7L);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("USER_010");
        verify(chain, never()).doFilter(request, response);
    }
}
