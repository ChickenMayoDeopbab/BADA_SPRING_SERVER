package ChickenMayoDeopbab.bada.domain.diagnosis.dto.request;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DiagnosisSubmitRequest {
    private Long userId;

    @NotBlank
    private String sessionId;

    @NotNull
    private DiagnosisType type;

    @NotEmpty
    private List<@NotNull @Min(1) @Max(5) Integer> answers;
}
