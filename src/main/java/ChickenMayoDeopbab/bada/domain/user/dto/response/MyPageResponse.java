package ChickenMayoDeopbab.bada.domain.user.dto.response;

public record MyPageResponse(
        String username,
        String profileImage
) {
    public static MyPageResponse of(String username, String profileImage) {
        return new MyPageResponse(username, profileImage);
    }
}