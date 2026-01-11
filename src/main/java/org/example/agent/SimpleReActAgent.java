package org.example.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.llm.LLMClient;
import org.example.llm.LLMResponse;
import org.example.tool.Tool;
import org.example.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 简单的 ReAct Agent 实现
 * 核心循环：Think → Act → Observe → Think → ...
 */
public class SimpleReActAgent {
    private static final Logger log = LoggerFactory.getLogger(SimpleReActAgent.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String systemPrompt;
    private final Map<String, Tool> tools;
    private final LLMClient llmClient;
    private final int maxSteps;

    // 对话历史
    private final List<Map<String,String>> conversationHistory = new ArrayList<>();

    public SimpleReActAgent(String systemPrompt, List<Tool> tools,
                            LLMClient llmClient, int maxSteps) {
        this.systemPrompt = systemPrompt;
        this.tools = new HashMap<>();
        for (Tool tool : tools) {
            this.tools.put(tool.getName(), tool);
        }
        this.llmClient = llmClient;
        this.maxSteps = maxSteps;
    }

    public AgentResult run(String userRequest) {
        log.info("🚀 Agent 开始执行，请求内容: {}", userRequest);

        conversationHistory.add(Map.of("role", "user", "content", userRequest));
        int currentStep = 0;
        String lastResult = null;

        while (currentStep < maxSteps) {
            currentStep++;
            log.info("📍 步骤 {}/{}", currentStep, maxSteps);
            try {
                ThinkResult thinkResult = think();
                if (thinkResult == null) {
                    log.error("思考返回空结果，正在重试...");
                    continue;
                }
                log.info("💭 Agent 思考中: {}", thinkResult.reasoning);

                // 2. 检查是否有工具调用
                if (thinkResult.toolCall == null) {
                    log.warn("响应中没有工具调用，提示使用工具...");
                    conversationHistory.add(Map.of(
                            "role", "assistant",
                            "content", thinkResult.reasoning != null ? thinkResult.reasoning : ""
                    ));
                    conversationHistory.add(Map.of(
                            "role", "user",
                            "content", "请使用工具继续执行。如果任务已完成，请使用 'terminate' 工具。"
                    ));
                    continue;
                }
                // 3. ACT: 执行工具
                ToolResult toolResult = act(thinkResult.toolCall);
                lastResult = toolResult.getOutput();

                log.info("🔧 工具 '{}' 执行结果: {}", thinkResult.toolCall.name, lastResult);

                // 4. 添加结果到对话历史
                String assistantMessage = String.format(
                        "我将使用 %s 工具。\n工具调用: %s\n执行结果: %s",
                        thinkResult.toolCall.name,
                        thinkResult.toolCall.toString(),
                        lastResult
                );
                conversationHistory.add(Map.of("role", "assistant", "content", assistantMessage));

                // 5. 检查是否应该终止
                if (toolResult.isShouldTerminate()) {
                    log.info("✅ Agent 执行成功完成");
                    return new AgentResult(AgentState.COMPLETED, lastResult, currentStep);
                }
            }catch (Exception e){
                log.error("步骤 {} 出错: {}", currentStep, e.getMessage(), e);
                conversationHistory.add(Map.of(
                        "role", "user",
                        "content", "发生错误: " + e.getMessage() + "。请尝试其他方法。"
                ));
            }
        }
        log.warn("⚠️ 已达到最大步数限制");
        return new AgentResult(AgentState.MAX_STEPS,
                lastResult != null ? lastResult : "达到最大步数限制，任务未完成",
                currentStep);
    }

    /**
     * Act: 执行工具调用
     */
    private ToolResult act(ToolCall toolCall) {
        Tool tool = tools.get(toolCall.name);
        if (tool == null) {
            return ToolResult.error("工具 " + toolCall.name + " 不存在");
        }
        return tool.execute(toolCall.arguments);
    }

    /**
     * 思考 让LLM 分析并决定下一步
     * @return 思考结果
     */
    private ThinkResult think() {
        // 构建完整的prompt
        String prompt = buildPrompt();

        // 调用LLM
        LLMResponse llmResponse = llmClient.chat(prompt);
        if (llmResponse == null || llmResponse.getContent() == null) {
            return null;
        }
        return parseThinkResult(llmResponse.getContent());
    }

    private ThinkResult parseThinkResult(String content) {
        ThinkResult result = new ThinkResult();
        try {
            // 尝试提取 JSON
            String json = extractJson(content);
            if (json == null) {
                result.reasoning = content;
                return result;
            }
            JsonNode root = objectMapper.readTree(json);

            // 提取 reasoning
            if (root.has("reasoning")) {
                result.reasoning = root.get("reasoning").asText();
            }
            // 提取工具调用
            if (root.has("tool")) {
                String toolName = root.get("tool").asText();
                Map<String, Object> args = new HashMap<>();

                if (root.has("arguments")) {
                    JsonNode argsNode = root.get("arguments");
                    argsNode.fields().forEachRemaining(entry ->
                            args.put(entry.getKey(), entry.getValue().asText())
                    );
                }

                result.toolCall = new ToolCall(toolName, args);
            }

        }catch (Exception e){
            log.warn("解析 LLM 响应为 JSON 失败: {}", e.getMessage());
            result.reasoning = content;
        }
        return null;
    }

    /**
     * 构建发送给 LLM 的 prompt
     */
    private String buildPrompt() {
        StringBuilder sb = new StringBuilder();

        // 1. 系统提示
        sb.append("系统提示:\n").append(systemPrompt).append("\n\n");

        // 2. 工具列表
        sb.append("可用工具:\n");
        for (Tool tool : tools.values()) {
            sb.append(String.format("- %s: %s\n  参数: %s\n",
                    tool.getName(),
                    tool.getDescription(),
                    tool.getParameterDescription()));
        }
        sb.append("\n");

        // 3. 输出格式说明
        sb.append("""
            响应格式要求:
            你必须以 JSON 对象格式响应:
            
            使用工具时:
            {
              "reasoning": "你的逐步思考过程，说明接下来要做什么",
              "tool": "工具名称",
              "arguments": {"参数1": "值1", "参数2": "值2"}
            }
            
            重要提示:
            - 当任务完成时，必须使用 'terminate' 工具
            - 在每次工具调用前提供清晰的推理过程
            - 只输出有效的 JSON，不要有其他文本
            
            """);

        // 4. 对话历史
        sb.append("对话历史:\n");
        for (Map<String, String> msg : conversationHistory) {
            sb.append(msg.get("role").toUpperCase()).append(": ")
                    .append(msg.get("content")).append("\n\n");
        }

        sb.append("助手: ");

        return sb.toString();
    }

    // 内部类：思考结果
    private static class ThinkResult {
        String reasoning;
        ToolCall toolCall;
    }
    // 内部类：工具调用
    private static class ToolCall {
        final String name;
        final Map<String, Object> arguments;

        ToolCall(String name, Map<String, Object> arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        @Override
        public String toString() {
            return String.format("%s(%s)", name, arguments);
        }
    }
    /**
     * * 从文本中提取 JSON
     */
    private String extractJson(String content) {
        // 找到第一个 { 和最后一个 }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }

        return null;
    }
}
