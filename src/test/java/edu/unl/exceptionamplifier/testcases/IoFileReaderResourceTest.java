package edu.unl.exceptionamplifier.testcases;

import org.junit.Test;
import static org.junit.Assert.*;
import edu.unl.exceptionamplifier.resource.IoFileReaderResource;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;

public class IoFileReaderResourceTest {
    @Test
    public void testReadFile() throws Exception {
        // 创建临时文件 sample.txt
        File file = new File("sample.txt");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Hello\nWorld\nTest");
        }
        IoFileReaderResource resource = new IoFileReaderResource();
        String content = resource.readFile("sample.txt");
        assertTrue(content.contains("Hello"));
        assertTrue(content.contains("World"));
        assertTrue(content.contains("Test"));
        // 清理
        file.delete();
    }

    @Test(expected = IOException.class)
    public void testReadFile_FileNotFound() throws Exception {
        IoFileReaderResource resource = new IoFileReaderResource();
        resource.readFile("/nonexistent/path/to/file.txt");
    }
}
