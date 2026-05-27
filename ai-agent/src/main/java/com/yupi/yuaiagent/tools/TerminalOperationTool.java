package com.yupi.yuaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 终端操作工具
 */
public class TerminalOperationTool {

    private final ToolExecutionSupport toolExecutionSupport;

    public TerminalOperationTool(ToolExecutionSupport toolExecutionSupport) {
        this.toolExecutionSupport = toolExecutionSupport;
    }

    @Tool(description = "Execute a command in the terminal")
    public String executeTerminalCommand(@ToolParam(description = "Command to execute in the terminal") String command) {
        return toolExecutionSupport.execute("TerminalOperationTool.executeTerminalCommand", command, () -> {
            toolExecutionSupport.validateTerminalCommand(command);
            String[] shellCommand = isWindows()
                    ? new String[]{"cmd.exe", "/c", command}
                    : new String[]{"/bin/sh", "-c", command};
            ProcessBuilder builder = new ProcessBuilder(shellCommand);
            Process process = builder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.append("Command execution failed with exit code: ").append(exitCode);
            }
            return output.toString();
        });
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
