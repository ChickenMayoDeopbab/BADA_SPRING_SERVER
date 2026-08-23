package ChickenMayoDeopbab.bada.domain.user.dto.response;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisResult;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;

import java.time.LocalDate;

public record MyPageResponse(
        String username,
        String email,
        String s3Key,
        CallPhobiaLevel level,
        String levelName,
        LocalDate diagnosisDate,
        double score,
        int trainCount,
        int attendance
) {
    public static MyPageResponse of(Users user, DiagnosisResult result, int trainCount, int attendanceCount) {
        return new MyPageResponse(
                user.getUsername(),
                user.getEmail(),
                user.getProfileImage(),
                result.getLevel(),
                result.getLevel().getName(),
                result.getUpdatedAt().toLocalDate(),
                result.getScore(),
                trainCount,
                attendanceCount
        );
    }
}