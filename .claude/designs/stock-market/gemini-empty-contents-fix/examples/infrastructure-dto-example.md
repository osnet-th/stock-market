# GeminiRequest 방어 로직 예시

```java
public record GeminiRequest(
        @JsonProperty("system_instruction") SystemInstruction systemInstruction,
        List<Content> contents,
        @JsonProperty("generationConfig") GenerationConfig generationConfig
) {
    public record SystemInstruction(List<Part> parts) {}
    public record Content(String role, List<Part> parts) {}
    public record Part(String text) {}
    public record GenerationConfig(double temperature) {}

    private static final Content FALLBACK_USER_CONTENT =
            new Content("user", List.of(new Part("위 시스템 지시사항에 따라 분석해주세요.")));

    public static GeminiRequest of(String systemPrompt, List<ChatMessage> messages) {
        List<Content> contents = messages.stream()
                .filter(m -> m.content() != null && !m.content().isBlank())
                .map(m -> new Content(m.role(), List.of(new Part(m.content()))))
                .toList();

        // Gemini API는 contents에 최소 1개 메시지 필요 — 방어 로직
        if (contents.isEmpty()) {
            contents = List.of(FALLBACK_USER_CONTENT);
        }

        return new GeminiRequest(
                new SystemInstruction(List.of(new Part(systemPrompt))),
                contents,
                new GenerationConfig(0.7)
        );
    }
}
```

## AnalysisTask에 toUserMessage() 추가

```java
public enum AnalysisTask {
    UNDERVALUATION(List.of(...)),
    TREND_SUMMARY(List.of(...)),
    RISK_DIAGNOSIS(List.of(...)),
    INVESTMENT_OPINION(List.of(...));

    // 기존 필드, 생성자, categories() 유지

    /**
     * 분석 버튼 클릭 시 Gemini에 전달할 사용자 메시지
     */
    public String toUserMessage() {
        return switch (this) {
            case UNDERVALUATION -> "이 종목의 저평가/고평가 여부를 판단해주세요.";
            case TREND_SUMMARY -> "이 종목의 실적 추세를 요약해주세요.";
            case RISK_DIAGNOSIS -> "이 종목의 리스크 요인을 진단해주세요.";
            case INVESTMENT_OPINION -> "이 종목의 투자 적정성 의견을 제시해주세요.";
        };
    }
}
```