package ChickenMayoDeopbab.bada.domain.auth.controller;

import ChickenMayoDeopbab.bada.domain.auth.dto.request.ChangePasswordRequest;
import ChickenMayoDeopbab.bada.domain.auth.dto.request.CheckUsernameRequest;
import ChickenMayoDeopbab.bada.domain.auth.dto.request.EmailRequest;
import ChickenMayoDeopbab.bada.domain.auth.dto.request.EmailVerificationRequest;
import  ChickenMayoDeopbab.bada.domain.auth.dto.request.LoginRequest;
import ChickenMayoDeopbab.bada.domain.auth.dto.request.OAuthCodeRequest;
import ChickenMayoDeopbab.bada.domain.auth.dto.request.RefreshRequest;
import ChickenMayoDeopbab.bada.domain.auth.dto.response.TokenResponse;
import ChickenMayoDeopbab.bada.domain.auth.service.AuthService;
import ChickenMayoDeopbab.bada.domain.auth.service.EmailService;
import ChickenMayoDeopbab.bada.domain.auth.service.OAuthRedirectUriResolver;
import ChickenMayoDeopbab.bada.domain.user.dto.request.SignUpRequest;
import ChickenMayoDeopbab.bada.domain.user.service.UserService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.jwt.MemberDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final AuthService authService;
    private final EmailService emailService;
    private final OAuthRedirectUriResolver redirectUriResolver;

    // 자체 회원가입
    @PostMapping("/signup")
    public ApiResponse<Void> signup(    
            @Valid @RequestBody SignUpRequest request) {
        userService.signUp(request);
        return ApiResponse.ok("회원가입에 성공했습니다.");
    }

    //자체 로그인
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        TokenResponse res = authService.login(request, response);

        return ApiResponse.ok(res, "로그인에 성공했습니다.");
    }

    // 중복 아이디 검사
    @PostMapping("/check/username")
    public ApiResponse<Boolean> checkUsername(@RequestBody @Valid CheckUsernameRequest request) {
        authService.checkUsername(request);
        return ApiResponse.ok(Boolean.TRUE);
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
        return ApiResponse.ok("로그아웃 되었습니다.");
    }

    // google login
    @GetMapping("/google")
    public void googleLogin(
            @RequestParam(required = false) String redirectUri,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        redirectUriResolver.remember(request, response, redirectUri);
        response.sendRedirect("/oauth2/authorization/google");
    }

    // naver login
    @GetMapping("/naver")
    public void naverLogin(
            @RequestParam(required = false) String redirectUri,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        redirectUriResolver.remember(request, response, redirectUri);
        response.sendRedirect("/oauth2/authorization/naver");
    }

    // 소셜 로그인 1회용 코드 → 토큰 교환
    @PostMapping("/oauth/token")
    public ApiResponse<TokenResponse> exchangeOAuthCode(
            @Valid @RequestBody OAuthCodeRequest request,
            HttpServletResponse response) {
        TokenResponse res = authService.exchangeOAuthCode(request.code(), response);

        return ApiResponse.ok(res, "로그인에 성공했습니다.");
    }

    // 이메일 전송
    @PostMapping("/email/send")
    public ApiResponse<?> sendEmail(
            @RequestBody @Valid EmailRequest requset){
        emailService.joinEmail(requset.email());
        return ApiResponse.ok("이메일이 전송되었습니다.");
    }

    // 이메일 인증
    @PostMapping("/email/check")
    public ApiResponse<?> checkEmail(
            @RequestBody @Valid EmailVerificationRequest request) {
        return emailService.checkEmail(request.email(), request.authNum());
    }

    // 비밀번호 변경
    @PatchMapping("/password")
    public ApiResponse<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(request);
    }

    // 아이디 찾기
    @PostMapping("/find-id")
    public ApiResponse<String> findId(
            @Valid @RequestBody EmailRequest request) {
        return authService.findId(request.email());
    }

    // 회원탈퇴
    @DeleteMapping("/withdraw")
    public ApiResponse<Void> withdraw() {
        userService.withdraw();
        return ApiResponse.ok("회원탈퇴에 성공했습니다.");
    }
}
