package ChickenMayoDeopbab.bada.domain.notification.model;

import ChickenMayoDeopbab.bada.domain.notification.entity.InAppNotificationType;

public enum CommunityReactionKind {
    LIKE(InAppNotificationType.POST_LIKE, "내 글에 좋아요를 눌렀어요."),
    RELATE(InAppNotificationType.POST_RELATE, "내 글에 공감돼요를 눌렀어요."),
    CHEER(InAppNotificationType.POST_CHEER, "내 글에 힘내요를 눌렀어요.");

    private final InAppNotificationType inAppNotificationType;
    private final String message;

    CommunityReactionKind(
            InAppNotificationType inAppNotificationType,
            String message
    ) {
        this.inAppNotificationType = inAppNotificationType;
        this.message = message;
    }

    public InAppNotificationType inAppNotificationType() {
        return inAppNotificationType;
    }

    public String message() {
        return message;
    }
}
