package ChickenMayoDeopbab.bada.domain.paymentintent.service;

import ChickenMayoDeopbab.bada.domain.paymentintent.dto.response.PaymentIntentStatusResponse;
import ChickenMayoDeopbab.bada.domain.paymentintent.dto.response.PaymentIntentSummaryResponse;
import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Role;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentIntentService {
    private static final List<EndReason> EXCLUDED_END_REASONS = List.of(EndReason.ERROR, EndReason.NO_AUDIO);
    private final UsersRepository usersRepository;
    private final TrainingRecordRepository trainingRecordRepository;

    @Transactional
    public PaymentIntentStatusResponse register() {
        Users user = getCurrentUser();
        user.intendPayment();

        return new PaymentIntentStatusResponse(
                user.isPaymentIntended()
        );
    }

    public PaymentIntentStatusResponse getMyStatus() {
        Users user = getCurrentUser();

        return new PaymentIntentStatusResponse(
                user.isPaymentIntended()
        );
    }

    public PaymentIntentSummaryResponse getSummary() {
        long totalUserCount =
                trainingRecordRepository.countDistinctTrainedUsers(
                        Role.USER,
                        EXCLUDED_END_REASONS
                );

        long paymentIntendedUserCount =
                trainingRecordRepository.countDistinctPaymentIntendedUsers(
                        Role.USER,
                        EXCLUDED_END_REASONS
                );

        return new PaymentIntentSummaryResponse(
                paymentIntendedUserCount,
                totalUserCount,
                calculateRate(paymentIntendedUserCount, totalUserCount)
        );
    }

    private BigDecimal calculateRate(long intended, long total) {
        if (total == 0) {
            return null;
        }

        return BigDecimal.valueOf(intended)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(total),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private Users getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return usersRepository.findByUsername(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new ApplicationException(
                                UsersStatusCode.USER_NOT_FOUND
                        )
                );
    }
}
