package com.yupi.yuaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class TerminalOperationToolTest {

    @Test
    void executeTerminalCommand() {
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool(
                new ToolExecutionSupport(Path.of("target/test-workspace"), 4000)
        );
        String command = System.getProperty("os.name", "").toLowerCase().contains("win") ? "dir" : "pwd";
        String result = terminalOperationTool.executeTerminalCommand(command);
        Assertions.assertNotNull(result);
    }
}
