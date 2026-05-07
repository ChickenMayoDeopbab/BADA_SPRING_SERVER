package ChickenMayoDeopbab.bada.domain.diagnosis.dto.request;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DiagnosisSubmitRequest {
    private Long userId;
    private String sessionId;
    private DiagnosisType type;
    private List<Integer> answers;
}
