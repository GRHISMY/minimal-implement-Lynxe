package org.example.tool;

import java.util.Map;

/**
 * 工具接口 - 函数是第一公民的体现
 * 每个工具就是一个可被Agent调用的函数
 */
public interface Tool {
    /**
     * 获取工具名称 (唯一标识)
     * @return
     */
    String getName();

    /**
     * 获取工具描述(给LLM理解用)
     * @return
     */
    String getDescription();

    /**
     * 参数说明(给LLM理解用)
     * @return
     */
    String getParameterDescription();

    /**
     * 执行工具
     * @param params
     * @return
     */
    ToolResult execute(Map<String,Object> params);

    /**
     * 是否是终止工具（调用后结束 Agent 循环）
     * @return
     */
    default boolean isTerminal() {
        return false;
    }
}
