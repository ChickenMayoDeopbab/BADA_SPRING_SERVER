package ChickenMayoDeopbab.bada.domain.paymentintent.dto.response;

import java.math.BigDecimal;

public record PaymentIntentSummaryResponse(
        long paymentIntendedUserCount,
        long totalUserCount,
        BigDecimal paymentIntentRate
) {
}
