package ChickenMayoDeopbab.bada.domain.diagnosis.service;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisQuestion;
import com.openai.client.OpenAIClient;
import com.openai.models.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class DiagnosisAiService {
    private final OpenAIClient openAIClient;

    public String generateSummary(List<DiagnosisQuestion> questions, List<Integer> answers) {
        StringBuilder userContent = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            userContent.append(String.format("Q%d. %s → %d점\n", i + 1, questions.get(i).getContent(), answers.get(i)));
        }

        ChatCompletionMessageParam systemMessage = ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder()
                        .content(ChatCompletionSystemMessageParam.Content.ofText(
                                """
                               콜포비아 자가진단 응답 결과를 종합적으로 분석하여 사용자의 전화 통화 성향과 콜포비아 수준을 2~3문장으로 요약해줘.
                                특정 어려움만 강조하지 말고, 전체 응답 경향을 바탕으로 전화 통화에 대한 부담감, 회피 경향, 긴장도 등을 균형 있게 설명해줘.
                                응답 점수가 낮은 경우에는 통화에 큰 부담을 느끼지 않는다는 내용이 자연스럽게 포함되어야 하며, 점수가 높은 경우에는 통화 상황에서 불안과 긴장을 경험할 수 있다는 내용을 포함해줘.
                                반드시 모든 문장은 '~합니다', '~느낍니다', '~보입니다' 등의 존댓말 형식으로만 작성해줘.
                                '~이다', '~있다', '~하는 편이다' 등의 평서체는 사용하지 마.
                                호칭, 공감 표현, 인삿말 없이 결과만 간결하게 작성해줘.
                               """
                        ))
                        .build()
        );

        ChatCompletionMessageParam userMessage = ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                        .content(ChatCompletionUserMessageParam.Content.ofText(userContent.toString()))
                        .build()
        );

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_4O_MINI)
                .addMessage(systemMessage)
                .addMessage(userMessage)
                .build();

        ChatCompletion completion = openAIClient.chat().completions().create(params);
        return completion.choices().get(0).message().content().orElse("");
    }
}