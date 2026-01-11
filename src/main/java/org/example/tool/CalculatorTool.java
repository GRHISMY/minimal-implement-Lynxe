package org.example.tool;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Map;

public class CalculatorTool implements Tool{

    private final ScriptEngine engine;

    public CalculatorTool() {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.engine = manager.getEngineByName("JavaScript");
    }

    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return "执行数学计算。支持 +、-、*、/ 和括号运算。";
    }

    @Override
    public String getParameterDescription() {
        return "expression (字符串): 要计算的数学表达式，例如 '(2 + 3) * 4'";
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        String expression = (String) params.get("expression");
        if (expression == null || expression.isBlank()) {
            return ToolResult.error("表达式不能为空");
        }
        try {
            Object result = engine.eval(expression);
            return ToolResult.success("计算结果: " + expression + " = " + result);
        } catch (Exception e) {
            return ToolResult.error("计算失败: " + e.getMessage());
        }
    }
}
