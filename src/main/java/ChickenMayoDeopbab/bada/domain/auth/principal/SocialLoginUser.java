package ChickenMayoDeopbab.bada.domain.auth.principal;

/**
 * 소셜 로그인 principal에 "이번 로그인에서 계정이 새로 만들어졌는지"를 실어 성공 핸들러까지 전달한다.
 * - 신규 여부는 회원 저장 시점(OAuthUserRegistrar)에만 알 수 있고, 성공 핸들러는 principal만 받기 때문이다.
 */
public interface SocialLoginUser {

    boolean isNewUser();
}
