package ChickenMayoDeopbab.bada.domain.user.service;

import ChickenMayoDeopbab.bada.domain.attendance.repository.AttendanceQueryRepository;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisResult;
import ChickenMayoDeopbab.bada.domain.diagnosis.exception.DiagnosisResultStatusCode;
import ChickenMayoDeopbab.bada.domain.diagnosis.repository.DiagnosisResultRepository;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.user.dto.request.SignUpRequest;
import ChickenMayoDeopbab.bada.domain.user.dto.request.UpdateMyPageRequest;
import ChickenMayoDeopbab.bada.domain.user.dto.response.MyPageResponse;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class UserService {
    private final UsersRepository usersRepository;
    private final DiagnosisResultRepository diagnosisResultRepository;
    private final AttendanceQueryRepository attendanceQueryRepository;
    private final TrainingRecordRepository trainingRecordRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    //회원가입
    public void signUp(SignUpRequest request) {
        validateDuplicates(request.username(), request.email());
        validateEmailVerified(request.email());

        usersRepository.save(request.toEntity(bCryptPasswordEncoder.encode(request.password())));
        redisTemplate.delete(request.email());
    }

    public void withdraw() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users user = usersRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));

        usersRepository.delete(user);
    }

    public ApiResponse<MyPageResponse> myPage() {
        Users user = getUserInfo();
        DiagnosisResult diagnosisResult = diagnosisResultRepository.findByUser(user)
                .orElseThrow(() -> new ApplicationException(DiagnosisResultStatusCode.DIAGNOSIS_RESULT_NOT_FOUND));
        int totalTraining = trainingRecordRepository.countByUser(user);
        int attendanceCount = attendanceQueryRepository.getTotalAttendance(user);

        return ApiResponse.ok(MyPageResponse.of(user, diagnosisResult, totalTraining, attendanceCount));
    }

    public ApiResponse<Void> updateMyPage(UpdateMyPageRequest request) {
        Users user = getUserInfo();

        user.update(request.name(), request.username(), request.s3Key());
        return ApiResponse.ok("프로필이 업데이트되었습니다.");
    }

    //  아이디 검증 및 이메일인 중복 검사
    private void validateDuplicates(String username, String email) {
        if (usersRepository.existsByUsername(username)) {
            throw new ApplicationException(UsersStatusCode.DUPLICATE_USERNAME);
        }
        if (usersRepository.existsByEmail(email)) {
            throw new ApplicationException(UsersStatusCode.DUPLICATE_EMAIL);
        }

    }

    // 이메일 인증을 한 사용자인지 검증
    private void validateEmailVerified(String email) {
        String status = redisTemplate.opsForValue().get(email);
        if (!"ACCESS".equals(status)) {
            throw new ApplicationException(UsersStatusCode.EMAIL_NOT_VERIFIED);
        }
    }

    private Users getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return usersRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));
    }
}
