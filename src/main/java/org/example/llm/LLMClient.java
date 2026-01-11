package org.example.llm;

/**
 * LLM客户端
 */
public interface LLMClient {
    /**
     * 发送聊天请求
     * @param prompt 提示词
     * @return LLM 相应
     */
    LLMResponse chat(String prompt);
}
