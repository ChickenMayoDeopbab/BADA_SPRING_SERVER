package ChickenMayoDeopbab.bada.domain.session.dto.request;

import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionClosedRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsNoAudioReasonFromFastApiCallback() throws Exception {
        String json = """
                {
                  "reason": "NO_AUDIO",
                  "transcript": [],
                  "silence_total": 12.5,
                  "shake_count": 0,
                  "good_segments": [],
                  "recording_url": null
                }
                """;

        SessionClosedRequest request = objectMapper.readValue(json, SessionClosedRequest.class);

        assertThat(request.reason()).isEqualTo(EndReason.NO_AUDIO);
    }
}
