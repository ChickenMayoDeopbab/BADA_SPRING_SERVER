package ChickenMayoDeopbab.bada.domain.session.port;

import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.session.model.ScenarioContext;
import ChickenMayoDeopbab.bada.domain.session.model.SessionContext;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TrainingRecordSessionRecordAdapterTest {
    @Test
    void preservesScenarioConditionsAtSessionStart() {
        UsersRepository users = mock(UsersRepository.class);
        TrainingRecordRepository records = mock(TrainingRecordRepository.class);
        when(users.findById(1L)).thenReturn(Optional.of(mock(Users.class)));
        ScenarioContext scenario = new ScenarioContext("예약", "직원", "안내", List.of(), null, "SCENARIO_V7");
        SessionContext context = new SessionContext(1L, 2L, SessionType.SCENARIO, null, 60,
                LocalDateTime.now().minusSeconds(30), scenario, 3, "HARD");

        new TrainingRecordSessionRecordAdapter(users, records, new ObjectMapper()).record(
                "session", context, EndReason.SCENARIO_DONE, List.of(), 0.0, 0, List.of(), null, null);

        ArgumentCaptor<TrainingRecord> saved = ArgumentCaptor.forClass(TrainingRecord.class);
        verify(records).save(saved.capture());
        assertThat(saved.getValue().getDifficulty()).isEqualTo("HARD");
        assertThat(saved.getValue().getScenarioVersion()).isEqualTo("SCENARIO_V7");
    }
}
