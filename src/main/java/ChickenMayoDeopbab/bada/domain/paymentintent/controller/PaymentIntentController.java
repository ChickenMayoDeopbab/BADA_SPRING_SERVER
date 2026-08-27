package ChickenMayoDeopbab.bada.domain.paymentintent.controller;

import ChickenMayoDeopbab.bada.domain.paymentintent.dto.response.PaymentIntentStatusResponse;
import ChickenMayoDeopbab.bada.domain.paymentintent.service.PaymentIntentService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payment-intents")
@RequiredArgsConstructor
public class PaymentIntentController {
    private final PaymentIntentService paymentIntentService;

    @PostMapping
    public ApiResponse<PaymentIntentStatusResponse> register() {
        return ApiResponse.ok(
                paymentIntentService.register(),
                "아직 결제 기능이 지원되지 않습니다. 마음껏 이용해 주세요."
        );
    }

    @GetMapping("/me")
    public ApiResponse<PaymentIntentStatusResponse> getMyStatus() {
        return ApiResponse.ok(
                paymentIntentService.getMyStatus()
        );
    }
}
