package edu.unl.exceptionamplifier.testcases;

import edu.unl.stock.StockTradingResource;
import edu.unl.stock.RemoteApiException;
import edu.unl.stock.InsufficientBalanceException;
import edu.unl.stock.PositionNotEnoughException;
import edu.unl.exceptionamplifier.collector.SequenceCollector;
import edu.unl.exceptionamplifier.builder.ExceptionalSpaceBuilder;
import edu.unl.exceptionamplifier.util.CoverageStatsReporter;
import edu.unl.exceptionamplifier.explorer.TestExplorer;

import org.junit.Test;

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
        List<String> exceptionTypes = Arrays.asList("IOException", "TimeoutException", "PositionNotEnoughException", "InsufficientBalanceException", "RemoteApiException");

        // 3. 生成所有mocking patterns
        ExceptionalSpaceBuilder builder = new ExceptionalSpaceBuilder();
        List<List<String>> patterns = builder.generateMockingPatterns(apiCalls, exceptionTypes, true);

        // 4. 用 TestExplorer 对每种异常组合进行测试
        CoverageStatsReporter coverageStats = new CoverageStatsReporter();
        TestExplorer explorer = new TestExplorer();
        explorer.explore(apiCalls, patterns, (pattern) -> {
            long startTime = System.currentTimeMillis();
            java.util.Map<String, Object> stat = new java.util.HashMap<>();
            stat.put("pattern", pattern);
            stat.put("apiCalls", apiCalls != null ? apiCalls.toString() : null);
            boolean isSuccess = false;
            try {
                StockTradingResource resource = new StockTradingResource(10000.0);
                resource.buyStock("AAPL", 10, 100.0);
                double balance = resource.getBalance();
                resource.sellStock("AAPL", 5, 120.0);
                int position = resource.getPosition("AAPL");
                stat.put("result", "success");
                stat.put("balance", balance);
                stat.put("position", position);
                isSuccess = true;
                System.out.println("[Test] Balance: " + balance + ", Position: " + position);
            } catch (RemoteApiException e) {
                stat.put("result", "exception");
                stat.put("exceptionType", e.getClass().getSimpleName());
                stat.put("exceptionMsg", e.getMessage());
                System.out.println("[Test] Caught RemoteApiException: " + e.getMessage());
            } catch (InsufficientBalanceException e) {
                stat.put("result", "exception");
                stat.put("exceptionType", e.getClass().getSimpleName());
                stat.put("exceptionMsg", e.getMessage());
                System.out.println("[Test] Caught InsufficientBalanceException: " + e.getMessage());
            } catch (PositionNotEnoughException e) {
                stat.put("result", "exception");
                stat.put("exceptionType", e.getClass().getSimpleName());
                stat.put("exceptionMsg", e.getMessage());
                System.out.println("[Test] Caught PositionNotEnoughException: " + e.getMessage());
            } catch (Exception e) {
                stat.put("result", "exception");
                stat.put("exceptionType", e.getClass().getSimpleName());
                stat.put("exceptionMsg", e.getMessage());
                System.out.println("[Test] Caught Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            } finally {
                long endTime = System.currentTimeMillis();
                stat.put("isSuccess", isSuccess);
                stat.put("timeCostMs", endTime - startTime);
                stat.put("timestamp", endTime);
            }
            String testName = "testAmplifiedBuyAndSell";
            String patternStr = pattern != null ? pattern.toString() : "";
            boolean covered = Boolean.TRUE.equals(stat.get("isSuccess"));
            coverageStats.addStat(testName, patternStr, covered);
        });

        coverageStats.printSummaryReport();
        coverageStats.printDetailReport();
        coverageStats.printCoverageTree();
    }

}
