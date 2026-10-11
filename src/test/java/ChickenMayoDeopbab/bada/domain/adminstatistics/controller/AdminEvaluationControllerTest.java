package ChickenMayoDeopbab.bada.domain.adminstatistics.controller;

import ChickenMayoDeopbab.bada.domain.adminstatistics.service.AdminEvaluationService;
import ChickenMayoDeopbab.bada.global.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminEvaluationControllerTest {
    private final AdminEvaluationService service = mock(AdminEvaluationService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminEvaluationController(service))
            .setControllerAdvice(new EvaluationRequestExceptionHandler(), new GlobalExceptionHandler()).build();

    @Test
    void malformedPoliciesReturnBadRequestBeforeExecution() throws Exception {
        mvc.perform(post("/admin/evaluations/runs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"베타\",\"userIds\":[1],\"policy\":{\"earlyCount\":0}}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/admin/evaluations/runs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"베타\",\"userIds\":[1],\"policy\":{\"windowMode\":\"INVALID\"}}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void missingPolicyIsRejected() throws Exception {
        mvc.perform(post("/admin/evaluations/runs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"베타\",\"userIds\":[1]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void nullSelectionAndOverrideEntriesAreRejectedBeforeExecution() throws Exception {
        for (String fragment : java.util.List.of("\"userIds\":[null]", "\"excludedUsers\":{\"1\":null}",
                "\"participantOverrides\":{\"1\":null}")) {
            mvc.perform(post("/admin/evaluations/runs").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"베타\",\"policy\":{}," + fragment + "}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
}
