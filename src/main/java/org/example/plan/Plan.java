package org.example.plan;

import java.util.ArrayList;
import java.util.List;

public class Plan {
    private final String id;
    private final String title;
    private final List<PlanStep> steps;

    public Plan(String id, String title) {
        this.id = id;
        this.title = title;
        this.steps = new ArrayList<>();
    }

    public Plan addStep(String requirement, List<String> allowedTools, int maxSteps) {
        steps.add(new PlanStep(steps.size(), requirement, allowedTools, maxSteps));
        return this;
    }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public List<PlanStep> getSteps() { return steps; }
}
