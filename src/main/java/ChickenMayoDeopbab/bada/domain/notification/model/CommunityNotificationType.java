package ChickenMayoDeopbab.bada.domain.notification.model;

public enum CommunityNotificationType {
    COMMENT("COMMUNITY_COMMENT", "내 게시글에 새로운 댓글이 달렸습니다."),
    REPLY("COMMUNITY_REPLY", "내 댓글에 새로운 답글이 달렸습니다."),
    REACTION("COMMUNITY_REACTION", null);

    private final String notificationType;
    private final String body;

    CommunityNotificationType(String notificationType, String body) {
        this.notificationType = notificationType;
        this.body = body;
    }

    public String notificationType() {
        return notificationType;
    }

    public String body() {
        return body;
    }
}
