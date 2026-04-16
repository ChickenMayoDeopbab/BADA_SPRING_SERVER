package ChickenMayoDeopbab.bada.domain.auth.handler;

import ChickenMayoDeopbab.bada.domain.auth.dto.response.TokenResponse;
import ChickenMayoDeopbab.bada.domain.auth.service.AuthService;
import ChickenMayoDeopbab.bada.domain.user.entity.Provider;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import ChickenMayoDeopbab.bada.global.jwt.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthService authService;
    private final UsersRepository usersRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        Provider provider = Provider.valueOf(authToken.getAuthorizedClientRegistrationId().toUpperCase());

        Users user = usersRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));

        String accessToken = authService.generateAccessToken(user.getUserId(), user.getRole(), response);
        String refreshToken = authService.generateRefreshToken(user.getUserId(), response);

        TokenResponse res =  new TokenResponse(accessToken, refreshToken);

        sendSuccessResponse(res, response);
    }

    private void sendSuccessResponse(
            TokenResponse tokenResponse,
            HttpServletResponse response) throws IOException {
        ApiResponse<TokenResponse> res = ApiResponse.ok(tokenResponse, "로그인에 성공했습니다.");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(res));
    }


}
