package ChickenMayoDeopbab.bada.domain.user.controller;

import ChickenMayoDeopbab.bada.domain.user.dto.request.UserModerationStatusRequest;
import ChickenMayoDeopbab.bada.domain.user.entity.UserStatus;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.service.UserModerationService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class InternalUserModerationControllerTest {

    private final UserModerationService service = mock(UserModerationService.class);
    private final InternalUserModerationController controller = controller();

    @Test
    void delegatesWithValidInternalSecret() {
        UserModerationStatusRequest request = new UserModerationStatusRequest(
                UserStatus.BANNED, null, "심각한 위반", 9L);

        ApiResponse<Void> response = controller.updateModerationStatus(7L, "test-internal-secret", request);

        verify(service).updateStatus(7L, request);
        assertThat(response.status()).isEqualTo(200);
    }

    @Test
    void rejectsInvalidInternalSecret() {
        UserModerationStatusRequest request = new UserModerationStatusRequest(
                UserStatus.BANNED, null, "심각한 위반", 9L);

        assertThatThrownBy(() -> controller.updateModerationStatus(7L, "wrong-secret", request))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getStatusCode())
                .isEqualTo(UsersStatusCode.INVALID_INTERNAL_SECRET);

        verifyNoInteractions(service);
    }

    private InternalUserModerationController controller() {
        InternalUserModerationController controller = new InternalUserModerationController(service);
        ReflectionTestUtils.setField(controller, "internalSecret", "test-internal-secret");
        return controller;
    }
}
