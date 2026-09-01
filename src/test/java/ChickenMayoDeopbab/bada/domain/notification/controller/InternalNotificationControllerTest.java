package ChickenMayoDeopbab.bada.domain.notification.controller;

import ChickenMayoDeopbab.bada.domain.notification.dto.request.CommunityNotificationRequest;
import ChickenMayoDeopbab.bada.domain.notification.exception.NotificationStatusCode;
import ChickenMayoDeopbab.bada.domain.notification.model.CommunityNotificationType;
import ChickenMayoDeopbab.bada.domain.notification.service.CommunityNotificationService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class InternalNotificationControllerTest {

    private final CommunityNotificationService service = mock(CommunityNotificationService.class);
    private final InternalNotificationController controller = controller();

    @Test
    void sendCommunityNotificationDelegatesWithValidSecret() {
        CommunityNotificationRequest request = request();

        ApiResponse<Void> response = controller.sendCommunityNotification(
                "test-internal-secret",
                request
        );

        verify(service).send(request);
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.message()).isEqualTo("커뮤니티 알림이 처리되었습니다.");
    }

    @Test
    void sendCommunityNotificationRejectsInvalidSecret() {
        assertThatThrownBy(() -> controller.sendCommunityNotification("wrong-secret", request()))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getStatusCode())
                .isEqualTo(NotificationStatusCode.INVALID_INTERNAL_SECRET);

        verifyNoInteractions(service);
    }

    private InternalNotificationController controller() {
        InternalNotificationController controller = new InternalNotificationController(service);
        ReflectionTestUtils.setField(controller, "internalSecret", "test-internal-secret");
        return controller;
    }

    private CommunityNotificationRequest request() {
        return new CommunityNotificationRequest(
                CommunityNotificationType.COMMENT,
                7L,
                8L,
                10L,
                25L
        );
    }
}
