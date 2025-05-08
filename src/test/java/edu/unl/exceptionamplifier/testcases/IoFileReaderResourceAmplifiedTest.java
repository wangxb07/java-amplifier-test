package edu.unl.exceptionamplifier.testcases;

import edu.unl.exceptionamplifier.collector.SequenceCollector;
import edu.unl.exceptionamplifier.builder.ExceptionalSpaceBuilder;
import edu.unl.exceptionamplifier.explorer.TestExplorer;
import edu.unl.exceptionamplifier.resource.IoFileReaderResource;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class IoFileReaderResourceAmplifiedTest {
    @Test
    public void testReadFileWithAmplification() throws IOException {
        // 1. 创建临时测试文件
        File tempFile = File.createTempFile("testfile", ".txt");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("Hello\nWorld\nTest");
        }

        // 2. 用 SequenceCollector 收集一次真实调用序列
        SequenceCollector collector = new SequenceCollector();
        // 这里模拟收集，实际可用AOP自动收集
        collector.collect("FileReader.new");
        collector.collect("BufferedReader.new");
        collector.collect("BufferedReader.readLine");
        collector.collect("BufferedReader.close");
        List<String> apiCalls = collector.getSequence();

        // 3. 构建异常类型集合
        List<String> exceptionTypes = Arrays.asList("IOException");

        // 4. 用 ExceptionalSpaceBuilder 生成所有mocking patterns
        ExceptionalSpaceBuilder builder = new ExceptionalSpaceBuilder();
        List<List<String>> patterns = builder.generateMockingPatterns(apiCalls, exceptionTypes);

        // 5. 用 TestExplorer 对每种异常组合进行测试
        TestExplorer explorer = new TestExplorer();
        explorer.explore(apiCalls, patterns, (pattern) -> {
            try {
                IoFileReaderResource resource = new IoFileReaderResource();
                String content = resource.readFile(tempFile.getAbsolutePath());
                System.out.println("[Test] Content: " + content);
            } catch (IOException e) {
                System.out.println("[Test] Caught IOException: " + e.getMessage());
            }
        });

        // 放大测试时动态指定异常类型
        String[] patternsArray = {"normal", "IOException", "TimeoutException"};
        for (String pattern : patternsArray) {
            IoFileReaderResource resource = new IoFileReaderResource();
            try {
                Method method = IoFileReaderResource.class.getDeclaredMethod("mockApiCall", String.class);
                method.setAccessible(true);
                method.invoke(resource, pattern); 
            } catch (Exception e) {
                // ignore
            }
            try {
                resource.readFile("test.txt");
            } catch (Exception e) {
                // 检查异常处理逻辑
                System.out.println("[Test] Caught Exception: " + e.getMessage());
            }
        }
    }
}
