package org.example.agent;

public class AgentResult {
    private final AgentState state;
    private final String result;
    private final int stepsUsed;

    public AgentResult(AgentState state, String result, int stepsUsed) {
        this.state = state;
        this.result = result;
        this.stepsUsed = stepsUsed;
    }

    public AgentState getState() { return state; }
    public String getResult() { return result; }
    public int getStepsUsed() { return stepsUsed; }

    @Override
    public String toString() {
        return String.format("AgentResult{state=%s, stepsUsed=%d, result='%s'}",
                state, stepsUsed, result);
    }
}
