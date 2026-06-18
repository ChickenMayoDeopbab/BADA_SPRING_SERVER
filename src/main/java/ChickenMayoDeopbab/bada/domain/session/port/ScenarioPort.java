package ChickenMayoDeopbab.bada.domain.session.port;

import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.session.model.ScenarioContext;

public interface ScenarioPort {
    ScenarioContext fetch(Long scenarioId, SessionType type);
}