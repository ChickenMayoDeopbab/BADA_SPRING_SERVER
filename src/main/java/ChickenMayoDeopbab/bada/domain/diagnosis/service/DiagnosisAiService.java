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
                               콜포비아 자가진단 결과를 분석해서 사용자가 전화 통화에서 어떤 부분을 특히 힘들어하는지 2~3문장으로 요약해줘.
                               반드시 모든 문장을 '~합니다', '~어려움을 느낍니다', '~부담을 느낍니다' 같은 존댓말 형식으로만 작성해줘.
                               '~이다', '~있다', '~하는 편이다' 같은 평서체 문장은 절대 사용하지 마.
                               호칭, 공감 표현, 인삿말 없이 진단 결과만 간결하게 작성해줘.
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