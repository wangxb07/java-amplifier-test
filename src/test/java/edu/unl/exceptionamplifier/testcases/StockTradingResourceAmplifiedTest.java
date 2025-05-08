package edu.unl.exceptionamplifier.testcases;

import edu.unl.exceptionamplifier.resource.StockTradingResource;
import edu.unl.exceptionamplifier.resource.RemoteApiException;
import edu.unl.exceptionamplifier.resource.InsufficientBalanceException;
import edu.unl.exceptionamplifier.resource.PositionNotEnoughException;
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
        List<String> exceptionTypes = Arrays.asList("IOException", "TimeoutException", "PositionNotEnoughException", "InsufficientBalanceException", "RemoteApiException");

        // 3. 生成所有mocking patterns
        ExceptionalSpaceBuilder builder = new ExceptionalSpaceBuilder();
        List<List<String>> patterns = builder.generateMockingPatterns(apiCalls, exceptionTypes, true);

        // 4. 用 TestExplorer 对每种异常组合进行测试
        CoverageStatsReporter coverageStats = new CoverageStatsReporter();
        TestExplorer explorer = new TestExplorer();
        explorer.explore(apiCalls, patterns, (pattern) -> {
            java.util.Map<String, Object> stat = new java.util.HashMap<>();
            stat.put("pattern", pattern);
            try {
                StockTradingResource resource = new StockTradingResource(10000.0, new PatternDrivenHttpApiCaller(pattern));
                resource.buyStock("AAPL", 10, 100.0);
                double balance = resource.getBalance();
                resource.sellStock("AAPL", 5, 120.0);
                int position = resource.getPosition("AAPL");
                stat.put("result", "success");
                stat.put("balance", balance);
                stat.put("position", position);
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
            }
            coverageStats.addStat(stat);
        });

        coverageStats.printSummaryReport();
        coverageStats.printDetailedReport(20);
        coverageStats.printCoverageTree();
    }

    // mock实现
    // 按pattern顺序注入异常的mock
    static class PatternDrivenHttpApiCaller implements edu.unl.exceptionamplifier.resource.HttpApiCaller {
        private final List<String> patterns;
        private int idx = 0;
        public PatternDrivenHttpApiCaller(List<String> patterns) { this.patterns = patterns; }
        @Override
        public void call(String apiCall) throws edu.unl.exceptionamplifier.resource.RemoteApiException {
            String type = idx < patterns.size() ? patterns.get(idx++) : "normal";
            switch (type) {
                case "IOException": throw new edu.unl.exceptionamplifier.resource.RemoteApiException("IO异常: " + apiCall);
                case "TimeoutException": throw new edu.unl.exceptionamplifier.resource.RemoteApiException("超时: " + apiCall);
                case "bad_gateway": throw new edu.unl.exceptionamplifier.resource.RemoteApiException("502 Bad Gateway: " + apiCall);
                case "disconnect": throw new edu.unl.exceptionamplifier.resource.RemoteApiException("网络断开: " + apiCall);
                case "invalid_json": /* 可扩展 */ break;
                case "normal": default: /* 正常 */
            }
        }
    }
    // 兼容原有简单mock
    static class MockHttpApiCaller implements edu.unl.exceptionamplifier.resource.HttpApiCaller {
        private final String scenario;
        public MockHttpApiCaller(String scenario) {
            this.scenario = scenario;
        }
        @Override
        public void call(String apiCall) throws edu.unl.exceptionamplifier.resource.RemoteApiException {
            switch (scenario) {
                case "timeout":
                    throw new edu.unl.exceptionamplifier.resource.RemoteApiException("网络超时: " + apiCall);
                case "bad_gateway":
                    throw new edu.unl.exceptionamplifier.resource.RemoteApiException("502 Bad Gateway: " + apiCall);
                case "disconnect":
                    throw new edu.unl.exceptionamplifier.resource.RemoteApiException("网络断开: " + apiCall);
                case "invalid_json":
                    // 假设业务解析时再抛异常，这里先通过
                    break;
                case "success":
                default:
                    // 正常
            }
        }
    }
}
