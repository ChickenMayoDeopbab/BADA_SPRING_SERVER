package ChickenMayoDeopbab.bada.domain.trainingrecord.dto.request;

import ChickenMayoDeopbab.bada.domain.session.enums.AiPersonality;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import ChickenMayoDeopbab.bada.global.exception.statuscode.CommonStatusCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public record TrainingRecordSearchQuery(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        SessionType sessionType, Long scenarioId, String scenarioVersion, String difficulty,
        AiPersonality aiPersonality, Integer scriptLevel
) {
    public TrainingRecordFilter validatedFilter() {
        boolean validPeriod = (from == null && to == null) || (from != null && to != null
                && !from.isBefore(LocalDate.of(2000, 1, 1)) && !to.isBefore(from)
                && !to.isAfter(LocalDate.now(ZoneId.of("Asia/Seoul"))) && ChronoUnit.DAYS.between(from, to) < 366);
        if (!validPeriod || (scenarioId != null && scenarioId <= 0)
                || (scriptLevel != null && (scriptLevel < 1 || scriptLevel > 3))
                || invalidText(scenarioVersion) || invalidText(difficulty)) {
            throw new ApplicationException(CommonStatusCode.INVALID_ARGUMENT, "훈련 기록 조회 기간 또는 조건이 올바르지 않습니다.");
        }
        return new TrainingRecordFilter(sessionType, scenarioId, scenarioVersion, difficulty, aiPersonality, scriptLevel);
    }

    public boolean hasFilters() {
        return from != null || to != null || sessionType != null || scenarioId != null || scenarioVersion != null
                || difficulty != null || aiPersonality != null || scriptLevel != null;
    }

    private boolean invalidText(String value) {
        return value != null && (value.isBlank() || value.length() > 255);
    }
}
