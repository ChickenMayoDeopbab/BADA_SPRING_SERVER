package ChickenMayoDeopbab.bada.domain.auth.controller;

import ChickenMayoDeopbab.bada.domain.auth.dto.request.LoginRequest;
import ChickenMayoDeopbab.bada.domain.auth.dto.response.LoginResponse;
import ChickenMayoDeopbab.bada.domain.auth.service.AuthService;
import ChickenMayoDeopbab.bada.domain.user.dto.request.SignUpRequest;
import ChickenMayoDeopbab.bada.domain.user.service.UserService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<Boolean> signup(@Valid @RequestBody SignUpRequest request) {
        userService.signUp(request);
        return ApiResponse.ok(true, "회원가입에 성공했습니다.");
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResponse res = authService.login(request, response);

        return ApiResponse.ok(res, "로그인에 성공했습니다.");
    }
}
