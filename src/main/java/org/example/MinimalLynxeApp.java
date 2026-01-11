package org.example;

import org.example.agent.AgentResult;
import org.example.agent.SimpleReActAgent;
import org.example.llm.DashScopeLLMClient;
import org.example.llm.LLMClient;
import org.example.plan.Plan;
import org.example.plan.PlanExecutor;
import org.example.tool.CalculatorTool;
import org.example.tool.SearchTool;
import org.example.tool.TerminateTool;
import org.example.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MinimalLynxeApp {
    private static final Logger log = LoggerFactory.getLogger(MinimalLynxeApp.class);

    public static void main(String[] args) {
        // 从环境变量获取 API Key
        String apiKey = "sk-b6c0b3223440431c8adb6ec827227f65";
        // 创建 LLM 客户端
        LLMClient llmClient = new DashScopeLLMClient(apiKey, "qwen-plus");
        // 准备工具
        List<Tool> tools = List.of(
                new CalculatorTool(),
                new SearchTool(),
                new TerminateTool()
        );

        System.out.println("=".repeat(60));
        System.out.println("示例 1：简单 Agent 模式");
        System.out.println("=".repeat(60));
        runSimpleAgent(llmClient, tools);

        System.out.println("\n" + "=".repeat(60));
        System.out.println("示例 2：Func-Agent 计划模式");
        System.out.println("=".repeat(60));
        runPlanMode(llmClient, tools);
    }

    /**
     * 示例 1：简单 Agent 模式
     * Agent 自由使用工具完成任务
     */
    private static void runSimpleAgent(LLMClient llmClient, List<Tool> tools) {
        String systemPrompt = """
            你是一个有帮助的 AI 助手。你可以使用工具来回答问题。
            请逐步思考，并使用合适的工具来解决用户的请求。
            当你得到最终答案时，请使用 'terminate' 工具来提交答案。
            """;

        SimpleReActAgent agent = new SimpleReActAgent(
                systemPrompt,
                tools,
                llmClient,
                10  // 最大 10 步
        );

        // 执行任务
        AgentResult result = agent.run("计算 (15 + 27) * 3，然后搜索一下关于 Java 的信息");

        System.out.println("\n📊 执行结果:");
        System.out.println("  状态: " + result.getState());
        System.out.println("  步数: " + result.getStepsUsed());
        System.out.println("  结果: " + result.getResult());
    }
    /**
     * 示例 2：Func-Agent 计划模式
     * 预定义计划步骤，按顺序执行
     */
    private static void runPlanMode(LLMClient llmClient, List<Tool> tools) {
        // 创建计划执行器
        PlanExecutor executor = new PlanExecutor(llmClient, tools);

        // 定义计划
        Plan plan = new Plan("plan-001", "数学计算与信息搜索")
                // 步骤 1：只允许使用计算器
                .addStep(
                        "计算 (100 - 37) * 2 + 15 的结果",
                        List.of("calculator", "terminate"),
                        5
                )
                // 步骤 2：只允许使用搜索
                .addStep(
                        "搜索关于 Spring Boot 的信息",
                        List.of("search", "terminate"),
                        5
                )
                // 步骤 3：总结
                .addStep(
                        "总结前两步的结果，给出最终答案",
                        List.of("terminate"),
                        3
                );

        // 执行计划
        PlanExecutor.PlanResult result = executor.execute(plan);

        System.out.println("\n📊 计划执行结果:");
        System.out.println("  计划ID: " + result.getPlanId());
        System.out.println("  状态: " + result.getState());
        System.out.println("  步骤数: " + result.getStepResults().size());

        for (PlanExecutor.StepResult stepResult : result.getStepResults()) {
            System.out.println("\n  步骤 " + stepResult.getStepIndex() + ":");
            System.out.println("    任务: " + stepResult.getRequirement());
            System.out.println("    状态: " + stepResult.getState());
            System.out.println("    用时: " + stepResult.getStepsUsed() + " 步");
            System.out.println("    结果: " + stepResult.getResult());
        }

        System.out.println("\n最终结果: " + result.getFinalResult());
    }
}