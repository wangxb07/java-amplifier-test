package edu.unl.exceptionamplifier.testcases;

import edu.unl.exceptionamplifier.resource.StockTradingResource;
import edu.unl.exceptionamplifier.collector.SequenceCollector;
import edu.unl.exceptionamplifier.builder.ExceptionalSpaceBuilder;
import edu.unl.exceptionamplifier.explorer.TestExplorer;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class StockTradingResourceAmplifiedTest {
    @Test
    public void testAmplifiedBuyAndSell() {
        // 1. 收集API调用序列（模拟，实际可用AOP自动收集）
        SequenceCollector collector = new SequenceCollector();
        collector.collect("StockExchangeApi.buy");
        collector.collect("DatabaseApi.getBalance");
        collector.collect("StockExchangeApi.sell");
        collector.collect("DatabaseApi.getPosition");
        List<String> apiCalls = collector.getSequence();

        // 2. 构建异常类型集合
        List<String> exceptionTypes = Arrays.asList("IOException", "TimeoutException");

        // 3. 生成所有mocking patterns
        ExceptionalSpaceBuilder builder = new ExceptionalSpaceBuilder();
        List<List<String>> patterns = builder.generateMockingPatterns(apiCalls, exceptionTypes);

        // 4. 用 TestExplorer 对每种异常组合进行测试
        TestExplorer explorer = new TestExplorer();
        explorer.explore(apiCalls, patterns, () -> {
            try {
                StockTradingResource resource = new StockTradingResource(10000.0);
                resource.buyStock("AAPL", 10, 100.0);
                double balance = resource.getBalance();
                resource.sellStock("AAPL", 5, 120.0);
                int position = resource.getPosition("AAPL");
                System.out.println("[Test] Balance: " + balance + ", Position: " + position);
            } catch (Exception e) {
                System.out.println("[Test] Caught Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        });

        // 放大测试时动态指定异常类型
        String[] patternsArray = {"normal", "IOException", "TimeoutException"};
        for (String pattern : patternsArray) {
            StockTradingResource resource = new StockTradingResource(10000.0);
            // 通过反射或框架机制将 pattern 传递给 resource.mockApiCall
            // 伪代码：resource.mockApiCall("StockExchangeApi.buy", pattern);
            try {
                resource.buyStock("AAPL", 1, 100.0);
            } catch (Exception e) {
                // 检查异常处理逻辑
            }
        }
    }
}
