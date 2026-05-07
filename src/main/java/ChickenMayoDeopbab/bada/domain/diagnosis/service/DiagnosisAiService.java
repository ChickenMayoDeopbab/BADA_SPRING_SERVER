package ChickenMayoDeopbab.bada.domain.diagnosis.service;

import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisQuestion;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class DiagnosisAiService {
    private final OpenAIClient openAIClient;

    public String generateSummary(List<DiagnosisQuestion> questions, List<Integer> answers) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음은 콜포비아(전화 공포증) 자가진단 결과입니다.\n");
        sb.append("각 문항과 사용자의 답변(1=매우 그렇지 않다, 5=매우 그렇다)을 분석해서\n");
        sb.append("사용자가 전화 통화에서 어떤 부분을 특히 힘들어하는지 2~3문장으로 요약해주세요.\n");
        sb.append("호칭(사용자님 등)이나 공감 표현 없이 진단 결과 내용만 작성해주세요.\n");
        sb.append("'~어려움을 느낍니다', '~부담을 느낍니다' 형식으로 간결하게 작성해주세요.\n\n");

        for (int i = 0; i < questions.size(); i++) {
            sb.append(String.format("Q%d. %s → %d점\n", i + 1, questions.get(i).getContent(), answers.get(i)));
        }

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_4O_MINI)
                .addUserMessage(sb.toString())
                .build();

        ChatCompletion completion = openAIClient.chat().completions().create(params);
        return completion.choices().get(0).message().content().orElse("");
    }
}