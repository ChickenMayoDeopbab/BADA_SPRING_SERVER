package ChickenMayoDeopbab.bada.domain.user.service;

import ChickenMayoDeopbab.bada.domain.user.dto.request.SignUpRequest;
import ChickenMayoDeopbab.bada.domain.user.dto.response.MyPageResponse;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(rollbackOn =  Exception.class)
public class UserService {
    private final UsersRepository usersRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    //회원가입
    public void signUp(SignUpRequest request) {
        isValidUser(request.username(), request.email());
        usersRepository.save(request.toEntity(request.username(), bCryptPasswordEncoder.encode(request.password()), request.email()));
    }

    public ApiResponse<MyPageResponse> getMyPage() {
        Users user = getUser();

        return ApiResponse.ok(MyPageResponse.of(user.getUsername(), user.getProfileImage()));
    }

    //  아이디 검증 및 이메일인 중복 검사
    private void isValidUser(String username, String email) {
        if (usersRepository.existsByUsername(username)) {
            throw new ApplicationException(UsersStatusCode.DUPLICATE_USERNAME);
        }
        if (usersRepository.existsByEmail(email)) {
            throw new ApplicationException(UsersStatusCode.DUPLICATE_EMAIL);
        }

    }

    private Users getUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return usersRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));
    }
}
