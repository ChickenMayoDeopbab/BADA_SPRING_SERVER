package ChickenMayoDeopbab.bada.domain.notification.dto.request;

import ChickenMayoDeopbab.bada.domain.notification.model.CommunityNotificationType;
import ChickenMayoDeopbab.bada.domain.notification.model.CommunityReactionKind;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CommunityNotificationRequest(
        @NotNull
        CommunityNotificationType type,

        @NotNull
        @Positive
        Long recipientUserId,

        @NotNull
        @Positive
        Long actorUserId,

        @NotNull
        @Positive
        Long postId,

        @Positive
        Long commentId,

        @Positive
        Long reactionId,

        CommunityReactionKind reactionKind
) {
    public CommunityNotificationRequest(
            CommunityNotificationType type,
            Long recipientUserId,
            Long actorUserId,
            Long postId,
            Long commentId
    ) {
        this(type, recipientUserId, actorUserId, postId, commentId, null, null);
    }

    @JsonIgnore
    @AssertTrue(message = "알림 유형별 이벤트 정보가 올바르지 않습니다.")
    public boolean isValidEventData() {
        if (type == null) {
            return true;
        }

        return switch (type) {
            case COMMENT, REPLY -> commentId != null
                    && reactionId == null
                    && reactionKind == null;
            case REACTION -> commentId == null
                    && reactionId != null
                    && reactionKind != null;
        };
    }
}
