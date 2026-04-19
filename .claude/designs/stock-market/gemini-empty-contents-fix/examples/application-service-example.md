# ChatService 수정 예시

```java
@Service
@RequiredArgsConstructor
public class ChatService {

    private final LlmPort llmPort;
    private final ChatContextBuilder contextBuilder;

    public Flux<String> chat(ChatRequest request) {
        String systemPrompt = contextBuilder.build(request);

        List<ChatMessage> allMessages = new ArrayList<>();
        if (request.messages() != null) {
            request.messages().stream()
                    .filter(m -> m.content() != null && !m.content().isBlank())
                    .forEach(allMessages::add);
        }

        // 분석 요청 시 빈 message 대신 분석 작업 설명을 사용자 메시지로 사용
        String userMessage = resolveUserMessage(request);
        allMessages.add(new ChatMessage("user", userMessage));

        return llmPort.stream(systemPrompt, allMessages);
    }

    private String resolveUserMessage(ChatRequest request) {
        // 사용자가 직접 입력한 메시지가 있으면 그대로 사용
        if (request.message() != null && !request.message().isBlank()) {
            return request.message();
        }

        // 분석 작업이 지정된 경우 작업에 맞는 기본 메시지 생성
        if (request.analysisTask() != null) {
            return request.analysisTask().toUserMessage();
        }

        return "";
    }
}
```