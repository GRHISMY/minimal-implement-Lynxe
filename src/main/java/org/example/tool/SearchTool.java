package org.example.tool;

import java.util.Map;

public class SearchTool implements Tool{
    @Override
    public String getName() {
        return "search";
    }

    @Override
    public String getDescription() {
        return "在网络上搜索指定主题的信息";
    }

    @Override
    public String getParameterDescription() {
        return "query (字符串): 搜索关键词";
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        String query = (String) params.get("query");
        if (query == null || query.isBlank()) {
            return ToolResult.error("查询关键字不能为空");
        }

        // 模拟搜索结果
        String result = String.format(
                "'%s' 的搜索结果:\n" +
                        "1. 百科: %s 是一个包含丰富信息的主题...\n" +
                        "2. 知识库: %s 的详细解释...\n" +
                        "3. 新闻: 关于 %s 的最新动态...",
                query, query, query, query
        );
        return ToolResult.success(result);
    }
}
