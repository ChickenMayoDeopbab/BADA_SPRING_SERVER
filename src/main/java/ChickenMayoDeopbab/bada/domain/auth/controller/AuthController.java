package ChickenMayoDeopbab.bada.domain.auth.controller;

import  ChickenMayoDeopbab.bada.domain.auth.dto.request.LoginRequest;
import ChickenMayoDeopbab.bada.domain.auth.dto.request.RefreshRequest;
import ChickenMayoDeopbab.bada.domain.auth.dto.response.TokenResponse;
import ChickenMayoDeopbab.bada.domain.auth.service.AuthService;
import ChickenMayoDeopbab.bada.domain.user.dto.request.SignUpRequest;
import ChickenMayoDeopbab.bada.domain.user.service.UserService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.jwt.MemberDetails;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final AuthService authService;

    // 자체 회원가입
    @PostMapping("/signup")
    public ApiResponse<Void> signup(
            @Valid @RequestBody SignUpRequest request) {
        userService.signUp(request);
        return ApiResponse.ok(null, "회원가입에 성공했습니다.");
    }

    //자체 로그인
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        TokenResponse res = authService.login(request, response);

        return ApiResponse.ok(res, "로그인에 성공했습니다.");
    }

    // 리프래시
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletResponse response) {
        TokenResponse res = authService.refresh(request, response);

        return ApiResponse.ok(res, "재발급되었습니다.");
    }

    //로그아웃
    @DeleteMapping("/signout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal MemberDetails memberDetails,
            HttpServletResponse response) {
        authService.signOut(memberDetails, response);
        return ApiResponse.ok(null, "로그아웃 되었습니다.");
    }

    @GetMapping("/google")
    public void googleLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/naver")
    public void naverLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/naver");
    }
}
