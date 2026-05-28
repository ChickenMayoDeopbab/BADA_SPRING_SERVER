package ChickenMayoDeopbab.bada.domain.user.dto.response;

public record MyPageResponse(
        String username,
        String email
) {
    public static MyPageResponse of(String username, String email) {
        return new MyPageResponse(username, email);
    }
}