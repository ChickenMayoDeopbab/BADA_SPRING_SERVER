package ChickenMayoDeopbab.bada.domain.user.service;

import ChickenMayoDeopbab.bada.domain.user.dto.request.SignUpRequest;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional(rollbackOn =  Exception.class)
public class UserService {
    private final UsersRepository usersRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public void signUp(SignUpRequest request) {
        isValidUser(request.username(), request.email());
        usersRepository.save(request.toEntity(request.username(), bCryptPasswordEncoder.encode(request.password()), request.email()));
    }

    private void isValidUser(String username, String email) {
        if (usersRepository.existsByUsername(username)) {
            throw new ApplicationException(UsersStatusCode.DUPLICATE_USERNAME);
        }
        if (usersRepository.existsByEmail(email)) {
            throw new ApplicationException(UsersStatusCode.DUPLICATE_EMAIL);
        }

    }
}
