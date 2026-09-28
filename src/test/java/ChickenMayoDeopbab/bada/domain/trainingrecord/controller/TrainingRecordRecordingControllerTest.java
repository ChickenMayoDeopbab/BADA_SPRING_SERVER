package ChickenMayoDeopbab.bada.domain.trainingrecord.controller;

import ChickenMayoDeopbab.bada.domain.trainingrecord.service.RecordingPlaybackService;
import ChickenMayoDeopbab.bada.domain.trainingrecord.service.TrainingRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrainingRecordRecordingControllerTest {

    private static final byte[] WAV = "RIFF0123456789".getBytes();

    private final RecordingPlaybackService recordingPlaybackService = mock(RecordingPlaybackService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new TrainingRecordController(mock(TrainingRecordService.class), recordingPlaybackService)
    ).build();

    @Test
    void 풀린_녹음을_wav로_내려주고_캐시하지_않는다() throws Exception {
        when(recordingPlaybackService.open(1L, "t")).thenReturn(WAV);

        mockMvc.perform(get("/api/v1/training-records/1/recording").param("token", "t"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "audio/wav"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().bytes(WAV));
    }

    @Test
    void 구간_요청에는_해당_바이트만_준다() throws Exception {
        // iOS 플레이어는 Range 요청으로 받아 가고, 잘한 구간(#t=) 재생도 이걸로 건너뛴다.
        when(recordingPlaybackService.open(1L, "t")).thenReturn(WAV);

        mockMvc.perform(get("/api/v1/training-records/1/recording").param("token", "t")
                        .header(HttpHeaders.RANGE, "bytes=4-7"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 4-7/14"))
                .andExpect(content().bytes("0123".getBytes()));
    }
}
