package ChickenMayoDeopbab.bada.domain.session.dto.response;

public record GoodSegments(
        Role role,
        String text
) {
    public enum Role {
        user,
        assistant
    }
}
