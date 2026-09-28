package ChickenMayoDeopbab.bada.domain.legalconsent.controller;

import ChickenMayoDeopbab.bada.domain.legalconsent.dto.request.AcceptLegalConsentRequest;
import ChickenMayoDeopbab.bada.domain.legalconsent.dto.response.LegalConsentStatusResponse;
import ChickenMayoDeopbab.bada.domain.legalconsent.service.LegalConsentService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Legal Consent", description = "이용약관 및 개인정보 관련 동의 API")
@RestController
@RequestMapping("/api/v1/legal-consents")
@RequiredArgsConstructor
public class LegalConsentController {

    private final LegalConsentService legalConsentService;

    @Operation(summary = "현재 사용자의 동의 상태 조회")
    @GetMapping("/status")
    public ApiResponse<LegalConsentStatusResponse> getStatus() {
        return ApiResponse.ok(legalConsentService.getStatus());
    }

    @Operation(summary = "이용약관 확인 및 개인정보 관련 동의 등록")
    @PostMapping("/accept")
    public ApiResponse<LegalConsentStatusResponse> accept(
            @Valid @RequestBody AcceptLegalConsentRequest request
    ) {
        return ApiResponse.ok(
                legalConsentService.accept(request),
                "약관 확인 및 동의 상태가 저장되었습니다."
        );
    }

    @Operation(summary = "민감정보 처리 동의 철회")
    @DeleteMapping("/sensitive-information")
    public ApiResponse<LegalConsentStatusResponse> withdrawSensitiveInformation() {
        return ApiResponse.ok(
                legalConsentService.withdrawSensitiveInformation(),
                "민감정보 처리 동의가 철회되었습니다."
        );
    }
}
