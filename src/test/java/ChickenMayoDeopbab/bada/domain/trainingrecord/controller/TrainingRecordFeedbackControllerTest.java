package ChickenMayoDeopbab.bada.domain.trainingrecord.controller;

import ChickenMayoDeopbab.bada.domain.session.enums.EndReason;
import ChickenMayoDeopbab.bada.domain.session.enums.SessionType;
import ChickenMayoDeopbab.bada.domain.session.model.GoodSegment;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.FeedbackResponse;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.PositiveFeedbackResponse;
import ChickenMayoDeopbab.bada.domain.trainingrecord.dto.response.TrainingRecordDetailResponse;
import ChickenMayoDeopbab.bada.domain.trainingrecord.entity.TrainingRecord;
import ChickenMayoDeopbab.bada.domain.trainingrecord.service.RecordingPlaybackService;
import ChickenMayoDeopbab.bada.domain.trainingrecord.service.TrainingRecordService;
import ChickenMayoDeopbab.bada.global.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// content 를 읽기 전에 배포된 앱은 구간 문구를 good_point 로만 읽는다.
class TrainingRecordFeedbackControllerTest {
    private static final String TITLE = "인사가 또렷했어요";
    private static final String CONTENT = "첫마디를 분명하게 해서 상대가 바로 알아들었어요.";

    private final TrainingRecordService service = mock(TrainingRecordService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new TrainingRecordController(service, mock(RecordingPlaybackService.class)))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    private final TrainingRecord record = TrainingRecord.builder()
            .sessionId("sess-1")
            .scenarioId(10L)
            .scenarioName("병원 예약 전화")
            .sessionType(SessionType.SCENARIO)
            .endReason(EndReason.SCENARIO_DONE)
            .startedAt(LocalDateTime.of(2026, 1, 1, 10, 0, 0))
            .endedAt(LocalDateTime.of(2026, 1, 1, 10, 1, 30))
            .durationSeconds(90L)
            .build();

    @Test
    void feedback_segments_carry_good_point_copy_of_content() throws Exception {
        returnFeedback(new GoodSegment(1.5, 3.0, TITLE, CONTENT));
        mvc.perform(get("/api/v1/training-records/feedback").param("scenarioId", "10").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goodSegments[0].title").value(TITLE))
                .andExpect(jsonPath("$.goodSegments[0].content").value(CONTENT))
                .andExpect(jsonPath("$.goodSegments[0].good_point").value(CONTENT));
    }

    @Test
    void record_detail_feedbacks_carry_good_point_copy_of_content() throws Exception {
        returnDetail(new GoodSegment(1.5, 3.0, TITLE, CONTENT));
        mvc.perform(get("/api/v1/training-records/1").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.positiveFeedbacks[0].title").value(TITLE))
                .andExpect(jsonPath("$.data.positiveFeedbacks[0].content").value(CONTENT))
                .andExpect(jsonPath("$.data.positiveFeedbacks[0].good_point").value(CONTENT));
    }

    @Test
    void good_point_is_empty_string_when_content_is_missing() throws Exception {
        GoodSegment noContent = new GoodSegment(1.5, 3.0, TITLE, null);
        returnFeedback(noContent);
        returnDetail(noContent);
        mvc.perform(get("/api/v1/training-records/feedback").param("scenarioId", "10").accept("application/json"))
                .andExpect(jsonPath("$.goodSegments[0].good_point").value(""));
        mvc.perform(get("/api/v1/training-records/1").accept("application/json"))
                .andExpect(jsonPath("$.data.positiveFeedbacks[0].good_point").value(""));
    }

    private void returnFeedback(GoodSegment segment) {
        when(service.getFeedback(10L)).thenReturn(FeedbackResponse.of(record, List.of(segment), null, List.of()));
    }

    private void returnDetail(GoodSegment segment) {
        when(service.getTrainingRecord(1L)).thenReturn(TrainingRecordDetailResponse.of(
                record, null, List.of(), List.of(PositiveFeedbackResponse.from(segment, null))));
    }
}
