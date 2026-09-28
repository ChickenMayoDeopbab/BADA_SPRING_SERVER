package ChickenMayoDeopbab.bada.domain.legalconsent.exception;

import ChickenMayoDeopbab.bada.global.exception.statuscode.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LegalConsentStatusCode implements StatusCode {
    SENSITIVE_INFORMATION_CONSENT_REQUIRED(
            HttpStatus.FORBIDDEN,
            "LEGAL_CONSENT_001",
            "민감정보 처리 동의가 필요한 기능입니다."
    ),
    PROFILE_IMAGE_CONSENT_REQUIRED(
            HttpStatus.FORBIDDEN,
            "LEGAL_CONSENT_002",
            "프로필 이미지 처리 동의가 필요합니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
