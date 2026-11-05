package ChickenMayoDeopbab.bada.domain.legalconsent.controller;

import ChickenMayoDeopbab.bada.domain.file.service.FileService;
import ChickenMayoDeopbab.bada.domain.legalconsent.service.LegalConsentService;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 생체신호 동의 전에 배포된 앱은 accept 요청에 biosignalAgreed 를 보내지 않는다.
class LegalConsentControllerTest {
    private static final String OLD_APP_ACCEPT = """
            {"termsOfServiceAgreed":true,"privacyPolicyAcknowledged":true,
             "sensitiveInformationAgreed":true,"profileImageAgreed":false}
            """;
    private static final String NEW_APP_ACCEPT = """
            {"termsOfServiceAgreed":true,"privacyPolicyAcknowledged":true,
             "sensitiveInformationAgreed":true,"profileImageAgreed":false,"biosignalAgreed":true}
            """;

    private final UsersRepository usersRepository = mock(UsersRepository.class);
    private final Users user = Users.builder().userId(7L).username("legal-consent-user").build();
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new LegalConsentController(new LegalConsentService(usersRepository, mock(FileService.class))))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @BeforeEach
    void setUp() {
        when(usersRepository.findByUsername("legal-consent-user")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("legal-consent-user", null));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void old_app_accept_without_biosignal_field_leaves_biosignal_consent_as_is() throws Exception {
        accept(OLD_APP_ACCEPT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sensitiveInformationAgreed").value(true))
                .andExpect(jsonPath("$.data.biosignalAgreed").value(false));

        Instant agreedAt = Instant.parse("2026-11-05T01:00:00Z");
        user.agreeBiosignal(agreedAt);

        accept(OLD_APP_ACCEPT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.biosignalAgreed").value(true));
        assertThat(user.getBiosignalAgreedAt()).isEqualTo(agreedAt);
    }

    @Test
    void biosignal_consent_and_withdrawal_appear_in_status_json() throws Exception {
        accept(NEW_APP_ACCEPT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.biosignalAgreed").value(true))
                .andExpect(jsonPath("$.data.biosignalAgreedAt").exists());

        mvc.perform(delete("/api/v1/legal-consents/biosignal").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.biosignalAgreed").value(false));

        mvc.perform(get("/api/v1/legal-consents/status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.biosignalAgreed").value(false))
                .andExpect(jsonPath("$.data.biosignalAgreedAt").exists())
                .andExpect(jsonPath("$.data.biosignalWithdrawnAt").exists())
                .andExpect(jsonPath("$.data.sensitiveInformationAgreed").value(true));
    }

    private ResultActions accept(String body) throws Exception {
        return mvc.perform(post("/api/v1/legal-consents/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
