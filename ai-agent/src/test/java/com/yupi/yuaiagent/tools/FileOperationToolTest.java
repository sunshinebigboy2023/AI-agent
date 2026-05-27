package com.yupi.yuaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

@Disabled("Integration test writes local files.")
class FileOperationToolTest {

    @Test
    void writeAndReadFile() {
        FileOperationTool fileOperationTool = new FileOperationTool(new ToolExecutionSupport(Path.of("target/test-workspace"), 4000));
        String fileName = "office-note.txt";
        String content = "AI office assistant workspace";
        String writeResult = fileOperationTool.writeFile(fileName, content);
        String readResult = fileOperationTool.readFile(fileName);
        Assertions.assertNotNull(writeResult);
        Assertions.assertNotNull(readResult);
    }
}
