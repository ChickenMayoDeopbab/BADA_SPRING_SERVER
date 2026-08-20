package ChickenMayoDeopbab.bada.domain.user.controller;

import ChickenMayoDeopbab.bada.domain.user.dto.request.UpdateMyPageRequest;
import ChickenMayoDeopbab.bada.domain.user.dto.response.MyPageResponse;
import ChickenMayoDeopbab.bada.domain.user.service.UserService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/mypage")
    public ApiResponse<MyPageResponse> mypage() {
        return userService.myPage();
    }

    @PatchMapping("/mypage")
    public ApiResponse<Void> updateMyPage(@Valid @RequestBody UpdateMyPageRequest request) {
        return userService.updateMyPage(request);
    }
}
