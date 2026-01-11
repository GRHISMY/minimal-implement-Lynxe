package org.example.llm;

public class LLMResponse {
    private final String content;
    private final int inputTokens;
    private final int outputTokens;

    public LLMResponse(String content, int inputTokens, int outputTokens) {
        this.content = content;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
    }

    public String getContent() { return content; }
    public int getInputTokens() { return inputTokens; }
    public int getOutputTokens() { return outputTokens; }
}
