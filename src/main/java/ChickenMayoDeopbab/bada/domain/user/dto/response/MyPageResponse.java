package ChickenMayoDeopbab.bada.domain.user.dto.response;

public record MyPageResponse(
        String username,
        String email,
        String s3Key
) {
    public static MyPageResponse of(String username, String email, String s3Key) {
        return new MyPageResponse(username, email, s3Key);
    }
}