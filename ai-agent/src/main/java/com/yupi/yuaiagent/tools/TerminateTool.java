package com.yupi.yuaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;

public class TerminateTool {

    @Tool(description = "Terminate the interaction when the request is met or the assistant cannot proceed further with the task.")
    public String doTerminate() {
        return "Task completed";
    }
}
