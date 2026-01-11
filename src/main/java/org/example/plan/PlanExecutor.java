package org.example.plan;

import org.example.agent.AgentResult;
import org.example.agent.AgentState;
import org.example.agent.SimpleReActAgent;
import org.example.llm.LLMClient;
import org.example.tool.TerminateTool;
import org.example.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanExecutor {

    private static final Logger log = LoggerFactory.getLogger(PlanExecutor.class);

    private final LLMClient llmClient;
    private final Map<String, Tool> allTools;

    public PlanExecutor(LLMClient llmClient, List<Tool> tools) {
        this.llmClient = llmClient;
        this.allTools = new HashMap<>();
        for (Tool tool : tools) {
            this.allTools.put(tool.getName(), tool);
        }
        // 确保 terminate 工具存在
        if (!this.allTools.containsKey("terminate")) {
            this.allTools.put("terminate", new TerminateTool());
        }
    }

    public PlanResult execute(Plan plan) {
        log.info("📋 开始执行计划: {} ({})", plan.getTitle(), plan.getId());
        List<StepResult> stepResults = new ArrayList<>();
        String previousResult = null;
        for (PlanStep step : plan.getSteps()) {
            log.info("📌 正在执行步骤 {}: {}", step.getIndex(), step.getRequirement());
            // 1. 筛选该步骤允许使用的工具
            List<Tool> stepTools = filterTools(step.getAllowedTools());
            // 2. 构建该步骤的系统提示
            String systemPrompt = buildStepPrompt(step, previousResult);
            // 3. 创建 Agent 执行该步骤
            SimpleReActAgent agent = new SimpleReActAgent(
                    systemPrompt,
                    stepTools,
                    llmClient,
                    step.getMaxSteps()
            );
            // 4. 运行 Agent
            AgentResult agentResult = agent.run(step.getRequirement());
            // 5. 记录结果
            StepResult stepResult = new StepResult(
                    step.getIndex(),
                    step.getRequirement(),
                    agentResult.getState(),
                    agentResult.getResult(),
                    agentResult.getStepsUsed()
            );
            stepResults.add(stepResult);
            log.info("✅ 步骤 {} 完成: {}", step.getIndex(), agentResult.getState());

            // 6. 传递结果给下一步
            previousResult = agentResult.getResult();
            // 7. 如果失败，停止执行
            if (agentResult.getState() == AgentState.FAILED) {
                log.error("❌ 步骤 {} 失败，停止计划执行", step.getIndex());
                break;
            }
        }

        // 确定最终状态
        boolean allCompleted = stepResults.stream()
                .allMatch(r -> r.getState() == AgentState.COMPLETED ||
                        r.getState() == AgentState.MAX_STEPS);
        AgentState finalState = allCompleted ? AgentState.COMPLETED : AgentState.FAILED;
        String finalResult = stepResults.isEmpty() ?
                "没有执行任何步骤" :
                stepResults.get(stepResults.size() - 1).getResult();

        log.info("📋 计划执行完成: {}", finalState);

        return new PlanResult(plan.getId(), finalState, stepResults, finalResult);
    }

    private List<Tool> filterTools(List<String> allowedToolNames) {
        List<Tool> result = new ArrayList<>();
        for (String name : allowedToolNames) {
            Tool tool = allTools.get(name);
            if (tool != null) {
                result.add(tool);
            }
        }
        // 始终添加 terminate 工具
        if (!allowedToolNames.contains("terminate")) {
            result.add(allTools.get("terminate"));
        }
        return result;
    }

    private String buildStepPrompt(PlanStep step, String previousResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个 AI 助手，正在执行多步骤计划的第 ").append(step.getIndex())
                .append(" 步。\n\n");
        sb.append("本步骤任务: ").append(step.getRequirement()).append("\n\n");

        if (previousResult != null) {
            sb.append("上一步结果:\n").append(previousResult).append("\n\n");
        }

        sb.append("完成此步骤后，请使用 'terminate' 工具提交结果。");

        return sb.toString();
    }


    // 内部类：步骤结果
    public static class StepResult {
        private final int stepIndex;
        private final String requirement;
        private final AgentState state;
        private final String result;
        private final int stepsUsed;

        public StepResult(int stepIndex, String requirement, AgentState state,
                          String result, int stepsUsed) {
            this.stepIndex = stepIndex;
            this.requirement = requirement;
            this.state = state;
            this.result = result;
            this.stepsUsed = stepsUsed;
        }

        public int getStepIndex() { return stepIndex; }
        public String getRequirement() { return requirement; }
        public AgentState getState() { return state; }
        public String getResult() { return result; }
        public int getStepsUsed() { return stepsUsed; }
    }
    public static class PlanResult {
        private final String planId;
        private final AgentState state;
        private final List<StepResult> stepResults;
        private final String finalResult;

        public PlanResult(String planId, AgentState state,
                          List<StepResult> stepResults, String finalResult) {
            this.planId = planId;
            this.state = state;
            this.stepResults = stepResults;
            this.finalResult = finalResult;
        }

        public String getPlanId() { return planId; }
        public AgentState getState() { return state; }
        public List<StepResult> getStepResults() { return stepResults; }
        public String getFinalResult() { return finalResult; }
    }
}
