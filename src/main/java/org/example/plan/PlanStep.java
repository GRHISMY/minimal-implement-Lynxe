package org.example.plan;

import java.util.List;

public class PlanStep {
    private final int index;
    private final String requirement;
    private final List<String> allowedTools;
    private final int maxSteps;

    public PlanStep(int index, String requirement, List<String> allowedTools, int maxSteps) {
        this.index = index;
        this.requirement = requirement;
        this.allowedTools = allowedTools;
        this.maxSteps = maxSteps;
    }

    public int getIndex() { return index; }
    public String getRequirement() { return requirement; }
    public List<String> getAllowedTools() { return allowedTools; }
    public int getMaxSteps() { return maxSteps; }
}
