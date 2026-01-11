package org.example.tool;

public class ToolResult {
    private final boolean success;
    private final String output;
    private final boolean shouldTerminate;

    public ToolResult(boolean success, String output) {
        this(success, output, false);
    }

    public ToolResult(boolean success, String output, boolean shouldTerminate) {
        this.success = success;
        this.output = output;
        this.shouldTerminate = shouldTerminate;
    }

    public boolean isSuccess() { return success; }
    public String getOutput() { return output; }
    public boolean isShouldTerminate() { return shouldTerminate; }

    public static ToolResult success(String output) {
        return new ToolResult(true, output);
    }

    public static ToolResult error(String message) {
        return new ToolResult(false, "Error: " + message);
    }

    public static ToolResult terminate(String finalAnswer) {
        return new ToolResult(true, finalAnswer, true);
    }

}
