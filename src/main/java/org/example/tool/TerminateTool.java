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
        return "";
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        return null;
    }

    @Override
    public boolean isTerminal() {
        return Tool.super.isTerminal();
    }
}
