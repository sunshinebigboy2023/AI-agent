package com.yupi.yuaiagent.agent.model;

import lombok.Builder;

@Builder
public record AgentStepRecord(
        int stepNumber,
        AgentStepPhase phase,
        String toolName,
        String toolArguments,
        String observation,
        String modelOutput,
        String errorMessage,
        long startTime,
        long endTime,
        long durationMs
) {
}
