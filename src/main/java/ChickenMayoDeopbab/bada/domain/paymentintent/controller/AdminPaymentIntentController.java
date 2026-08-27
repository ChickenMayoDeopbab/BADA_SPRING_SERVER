package ChickenMayoDeopbab.bada.domain.paymentintent.controller;

import ChickenMayoDeopbab.bada.domain.paymentintent.dto.response.PaymentIntentSummaryResponse;
import ChickenMayoDeopbab.bada.domain.paymentintent.service.PaymentIntentService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/statistics/payment-intents")
@RequiredArgsConstructor
public class AdminPaymentIntentController {
    private final PaymentIntentService paymentIntentService;

    @GetMapping("/summary")
    public ApiResponse<PaymentIntentSummaryResponse> getSummary() {
        return ApiResponse.ok(
                paymentIntentService.getSummary()
        );
    }
}
