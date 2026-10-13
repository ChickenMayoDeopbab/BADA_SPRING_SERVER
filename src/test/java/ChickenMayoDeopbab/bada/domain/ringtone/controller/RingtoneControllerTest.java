package ChickenMayoDeopbab.bada.domain.ringtone.controller;

import ChickenMayoDeopbab.bada.domain.file.exception.FileStatusCode;
import ChickenMayoDeopbab.bada.domain.ringtone.dto.request.UpdateRingtoneSettingRequest;
import ChickenMayoDeopbab.bada.domain.ringtone.dto.response.RingtoneResponse;
import ChickenMayoDeopbab.bada.domain.ringtone.dto.response.RingtoneSettingResponse;
import ChickenMayoDeopbab.bada.domain.ringtone.service.RingtoneService;
import ChickenMayoDeopbab.bada.global.config.PublicEndpoints;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import ChickenMayoDeopbab.bada.global.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RingtoneControllerTest {

    private final RingtoneService service = mock(RingtoneService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new RingtoneController(service))
            .defaultRequest(get("/").accept(MediaType.APPLICATION_JSON))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void uploadUsesMultipartAndReturnsCreated() throws Exception {
        when(service.upload(any())).thenReturn(new RingtoneResponse(12L, "ring.wav", "fresh-url"));

        mvc.perform(multipart("/api/v1/ringtones")
                        .file(new MockMultipartFile("file", "ring.wav", "audio/wav", new byte[]{1})))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.fileId").value(12));
    }

    @Test
    void settingsPathIsResolvedBeforeFileIdPathAndKeepsExplicitNulls() throws Exception {
        when(service.getSetting()).thenReturn(RingtoneSettingResponse.defaultSetting());

        mvc.perform(get("/api/v1/ringtones/settings"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.defaultRingtone").value(true))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"fileId\":null")));
        verify(service, never()).get(any());
    }

    @Test
    void nullFileIdRestoresDefault() throws Exception {
        when(service.updateSetting(new UpdateRingtoneSettingRequest(null))).thenReturn(RingtoneSettingResponse.defaultSetting());

        mvc.perform(put("/api/v1/ringtones/settings").contentType(MediaType.APPLICATION_JSON).content("{\"fileId\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.defaultRingtone").value(true));
    }

    @Test
    void invalidFileIdIsRejectedBeforeService() throws Exception {
        mvc.perform(put("/api/v1/ringtones/settings").contentType(MediaType.APPLICATION_JSON).content("{\"fileId\":0}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void missingUploadFileIsBadRequest() throws Exception {
        mvc.perform(multipart("/api/v1/ringtones")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void otherUsersRingtoneReturnsNotFound() throws Exception {
        when(service.get(12L)).thenThrow(ApplicationException.of(FileStatusCode.FILE_NOT_FOUND));

        mvc.perform(get("/api/v1/ringtones/12")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("FILE_003"));
    }

    @Test
    void multipartTransportLimitReturnsPayloadTooLarge() throws Exception {
        when(service.upload(any())).thenThrow(new MaxUploadSizeExceededException(10 * 1024 * 1024));

        mvc.perform(multipart("/api/v1/ringtones").file(new MockMultipartFile("file", new byte[]{1})))
                .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.error.code").value("FILE_007"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/ringtones", "/api/v1/ringtones/12", "/api/v1/ringtones/settings"})
    void ringtoneRoutesAreNotPublic(String path) {
        for (String method : new String[]{"GET", "POST", "PUT", "DELETE"}) {
            MockHttpServletRequest request = new MockHttpServletRequest(method, path);
            request.setServletPath(path);
            assertThat(PublicEndpoints.matches(request)).isFalse();
        }
    }
}
