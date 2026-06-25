package ChickenMayoDeopbab.bada.domain.session.port;

import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.session.model.ScenarioContext;
import ChickenMayoDeopbab.bada.domain.session.model.ScriptTurn;
import org.springframework.stereotype.Component;

import java.util.List;

// 더미 스크립트임
@Component
public class StubScenarioProvider implements ScenarioPort {

    @Override
    public ScenarioContext fetch(Long scenarioId, SessionType type) {
        if (type == SessionType.WARMUP) {
            return new ScenarioContext("워밍업 통화", "친근한 지인", "You are a friendly acquaintance making a warm-up phone call.", List.of(
                    new ScriptTurn(1, "가볍게 인사하고 안부를 묻는다", "편하게 인사해보세요"),
                    new ScriptTurn(2, "오늘 하루 어땠는지 짧게 되묻는다", ""),
                    new ScriptTurn(3, "잘 지내라는 인사로 통화를 마무리한다", "")
            ));
        }
        return new ScenarioContext("병원 예약 변경", "병원 접수 직원", "You are a hospital receptionist handling appointment changes.", List.of(
                new ScriptTurn(1, "전화를 받고 용건을 묻는다", "예약을 변경하고 싶다고 말해보세요"),
                new ScriptTurn(2, "기존 예약자명과 날짜를 확인한다", ""),
                new ScriptTurn(3, "변경 희망 일정을 받아 확정하고 마무리한다", "")
        ));
    }
}
