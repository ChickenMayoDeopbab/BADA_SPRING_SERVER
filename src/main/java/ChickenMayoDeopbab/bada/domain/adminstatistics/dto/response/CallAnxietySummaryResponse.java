package ChickenMayoDeopbab.bada.domain.adminstatistics.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

public record CallAnxietySummaryResponse(
        OffsetDateTime generatedAt,
        String scope,
        String scoringVersion,
        long totalUserCount,
        long eligibleUserCount,
        long improvedUserCount,
        @Schema(description = "앱 내부 합성 지수 감소 비율. 동일 척도의 전화 불안 전후 개선율이 아님. 효과 평가는 /admin/evaluations/runs 사용")
        BigDecimal improvementRate,
        BigDecimal averageInitialScore,
        BigDecimal averageCurrentIndex,
        BigDecimal averageScoreChange,
        long levelImprovedUserCount,
        BigDecimal levelImprovementRate,
        String selfAssessmentVersion,
        long reassessedUserCount,
        long selfReportEligibleUserCount,
        long selfReportImprovedUserCount,
        @Schema(description = "전체 기간 첫·마지막 검사에 대한 기존 집계. 대상·기간·설문 버전을 지정하는 새 평가 API 사용 권장")
        BigDecimal selfReportImprovementRate,
        BigDecimal averageInitialSelfReportScore,
        BigDecimal averageLatestSelfReportScore,
        BigDecimal averageSelfReportScoreChange
) {
}
