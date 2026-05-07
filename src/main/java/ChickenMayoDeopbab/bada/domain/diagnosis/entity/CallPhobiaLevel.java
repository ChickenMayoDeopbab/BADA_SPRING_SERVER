package ChickenMayoDeopbab.bada.domain.diagnosis.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CallPhobiaLevel {
    LEVEL_1("통화 안정형",
            "전화 통화에 대한 부담이 거의 없는 상태예요. 대부분의 상황에서 자연스럽게 전화를 받고 대화할 수 있으며, 돌발 상황에도 비교적 안정적으로 대응하는 편입니다."),
    LEVEL_2("통화 적응형",
            "전화 통화에 약간의 긴장감은 느끼지만, 전반적으로 잘 적응하고 있는 상태예요. 익숙하지 않은 상황에서는 잠시 망설일 수 있지만, 통화를 크게 회피하지는 않습니다."),
    LEVEL_3("통화 긴장형",
            "전화가 오거나 걸어야 하는 상황에서 긴장과 부담을 자주 느끼는 상태예요.통화 전 할 말을 미리 정리하거나, 실수에 대한 걱정을 자주 하는 경향이 있습니다."),
    LEVEL_4("통화 경계형",
            "전화 통화 자체를 신경 쓰고 조심하게 되는 상태예요.벨소리나 부재중 전화에도 스트레스를 느끼며, 예상치 못한 통화 상황을 부담스럽게 받아들이는 경우가 많습니다."),
    LEVEL_5("통화 회피형",
            "전화 통화에 대한 불안으로 인해 통화를 피하려는 경향이 강한 상태예요.가능하면 문자나 메신저를 우선 사용하며, 전화 자체가 큰 스트레스와 긴장으로 이어질 수 있습니다.");

    private final String name;
    private final String description;
}
