package org.example.tool;

import java.util.Map;

public class TerminateTool implements Tool {

    @Override
    public String getName() {
        return "terminate";
    }

    @Override
    public String getDescription() {
        return "当任务完成时调用此工具，提供最终答案。";
    }

    @Override
    public String getParameterDescription() {
        return "answer (字符串): 返回给用户的最终答案";
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        String answer = (String) params.getOrDefault("answer","任务已完成");
        return ToolResult.terminate(answer);
    }

    @Override
    public boolean isTerminal() {
        return true;
    }
}
