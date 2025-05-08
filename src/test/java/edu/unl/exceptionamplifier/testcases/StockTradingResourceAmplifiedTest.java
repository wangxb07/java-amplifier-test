package edu.unl.exceptionamplifier.testcases;

import edu.unl.stock.StockTradingResource;

import edu.unl.stock.InsufficientBalanceException;
import edu.unl.stock.PositionNotEnoughException;
import edu.unl.exceptionamplifier.collector.SequenceCollector;
import edu.unl.exceptionamplifier.builder.ExceptionalSpaceBuilder;
import edu.unl.exceptionamplifier.util.CoverageStatsReporter;
import edu.unl.exceptionamplifier.explorer.TestExplorer;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import edu.unl.stock.StockTradingRepository;
import edu.unl.stock.MarketDataService;
import java.io.IOException;
import java.sql.SQLException;

import java.util.Arrays;
import java.util.List;

public class StockTradingResourceAmplifiedTest {
    @Test
    public void testBuyStock_RealtimePriceIOException() throws Exception {
        // Mock MarketDataService 抛 IOException
        MarketDataService mockMarketData = mock(MarketDataService.class);
        when(mockMarketData.getRealtimePrice(anyString())).thenThrow(new IOException("模拟HTTP请求失败"));
        StockTradingRepository repo = new StockTradingRepository();
        repo.initDatabase(10000.0);
        edu.unl.stock.StockTradingService service = new edu.unl.stock.StockTradingService(repo, mockMarketData);
        StockTradingResource resource = new StockTradingResource(service);
        try {
            resource.buyStock("AAPL", 10);
            org.junit.Assert.fail("应当抛出 IOException");
        } catch (IOException e) {
            // 期望抛出
        }
    }

    @Test
    public void testBuyStock_TransactionSQLException() throws Exception {
        // Mock StockTradingRepository 抛 SQLException
        StockTradingRepository mockRepo = mock(StockTradingRepository.class);
        when(mockRepo.getBalance()).thenReturn(10000.0);
        when(mockRepo.getPosition(anyString())).thenReturn(0);
        doThrow(new SQLException("模拟事务失败")).when(mockRepo).executeTradeTransaction(anyString(), anyInt(), anyDouble(), anyString());
        MarketDataService marketData = mock(MarketDataService.class);
        when(marketData.getRealtimePrice(anyString())).thenReturn(100.0);
        edu.unl.stock.StockTradingService service = new edu.unl.stock.StockTradingService(mockRepo, marketData);
        StockTradingResource resource = new StockTradingResource(service);
        try {
            resource.buyStock("AAPL", 10);
            org.junit.Assert.fail("应当抛出 SQLException");
        } catch (SQLException e) {
            // 期望抛出
        }
    }
    @Test
    public void testBuyStock_GetBalanceIOException() throws Exception {
        StockTradingRepository mockRepo = mock(StockTradingRepository.class);
        when(mockRepo.getBalance()).thenThrow(new IOException("模拟 getBalance 失败"));
        MarketDataService marketData = mock(MarketDataService.class);
        when(marketData.getRealtimePrice(anyString())).thenReturn(100.0);
        edu.unl.stock.StockTradingService service = new edu.unl.stock.StockTradingService(mockRepo, marketData);
        StockTradingResource resource = new StockTradingResource(service);
        try {
            resource.buyStock("AAPL", 10);
            org.junit.Assert.fail("应当抛出 IOException");
        } catch (IOException e) {
            // 期望抛出
        }
    }

    @Test
    public void testBuyStock_GetPositionIOException() throws Exception {
        StockTradingRepository mockRepo = mock(StockTradingRepository.class);
        when(mockRepo.getBalance()).thenReturn(10000.0);
        when(mockRepo.getPosition(anyString())).thenThrow(new IOException("模拟 getPosition 失败"));
        MarketDataService marketData = mock(MarketDataService.class);
        when(marketData.getRealtimePrice(anyString())).thenReturn(100.0);
        edu.unl.stock.StockTradingService service = new edu.unl.stock.StockTradingService(mockRepo, marketData);
        StockTradingResource resource = new StockTradingResource(service);
        try {
            resource.buyStock("AAPL", 10);
            org.junit.Assert.fail("应当抛出 IOException");
        } catch (IOException e) {
            // 期望抛出
        }
    }

    @Test
    public void testBuyStock_TransactionRuntimeException() throws Exception {
        StockTradingRepository mockRepo = mock(StockTradingRepository.class);
        when(mockRepo.getBalance()).thenReturn(10000.0);
        when(mockRepo.getPosition(anyString())).thenReturn(0);
        doThrow(new RuntimeException("模拟事务运行时异常")).when(mockRepo).executeTradeTransaction(anyString(), anyInt(), anyDouble(), anyString());
        MarketDataService marketData = mock(MarketDataService.class);
        when(marketData.getRealtimePrice(anyString())).thenReturn(100.0);
        edu.unl.stock.StockTradingService service = new edu.unl.stock.StockTradingService(mockRepo, marketData);
        StockTradingResource resource = new StockTradingResource(service);
        try {
            resource.buyStock("AAPL", 10);
            org.junit.Assert.fail("应当抛出 RuntimeException");
        } catch (RuntimeException e) {
            // 期望抛出
        }
    }

    @Test
    public void testBuyStock_RealtimePriceRuntimeException() throws Exception {
        MarketDataService mockMarketData = mock(MarketDataService.class);
        when(mockMarketData.getRealtimePrice(anyString())).thenThrow(new RuntimeException("模拟HTTP运行时异常"));
        StockTradingRepository repo = new StockTradingRepository();
        repo.initDatabase(10000.0);
        edu.unl.stock.StockTradingService service = new edu.unl.stock.StockTradingService(repo, mockMarketData);
        StockTradingResource resource = new StockTradingResource(service);
        try {
            resource.buyStock("AAPL", 10);
            org.junit.Assert.fail("应当抛出 RuntimeException");
        } catch (RuntimeException e) {
            // 期望抛出
        }
    }

    @Test
    public void testBuyStock_TransactionInsufficientBalanceException() throws Exception {
        StockTradingRepository mockRepo = mock(StockTradingRepository.class);
        when(mockRepo.getBalance()).thenReturn(10000.0);
        when(mockRepo.getPosition(anyString())).thenReturn(0);
        doThrow(new edu.unl.stock.InsufficientBalanceException("余额不足")).when(mockRepo).executeTradeTransaction(anyString(), anyInt(), anyDouble(), anyString());
        MarketDataService marketData = mock(MarketDataService.class);
        when(marketData.getRealtimePrice(anyString())).thenReturn(100.0);
        edu.unl.stock.StockTradingService service = new edu.unl.stock.StockTradingService(mockRepo, marketData);
        StockTradingResource resource = new StockTradingResource(service);
        try {
            resource.buyStock("AAPL", 10);
            org.junit.Assert.fail("应当抛出 InsufficientBalanceException");
        } catch (edu.unl.stock.InsufficientBalanceException e) {
            // 期望抛出
        }
    }

    @Test
    public void testBuyStock_RealtimePriceNegative() throws Exception {
        MarketDataService mockMarketData = mock(MarketDataService.class);
        when(mockMarketData.getRealtimePrice(anyString())).thenReturn(-100.0);
        StockTradingRepository repo = new StockTradingRepository();
        repo.initDatabase(10000.0);
        edu.unl.stock.StockTradingService service = new edu.unl.stock.StockTradingService(repo, mockMarketData);
        StockTradingResource resource = new StockTradingResource(service);
        try {
            resource.buyStock("AAPL", 10);
            org.junit.Assert.fail("应当抛出 IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // 期望抛出
        }
    }

    @Test
    public void testBuyStock_TransactionTimeoutException() throws Exception {
        StockTradingRepository mockRepo = mock(StockTradingRepository.class);
        when(mockRepo.getBalance()).thenReturn(10000.0);
        when(mockRepo.getPosition(anyString())).thenReturn(0);
        doThrow(new java.util.concurrent.TimeoutException("模拟事务超时")).when(mockRepo).executeTradeTransaction(anyString(), anyInt(), anyDouble(), anyString());
        MarketDataService marketData = mock(MarketDataService.class);
        when(marketData.getRealtimePrice(anyString())).thenReturn(100.0);
        edu.unl.stock.StockTradingService service = new edu.unl.stock.StockTradingService(mockRepo, marketData);
        StockTradingResource resource = new StockTradingResource(service);
        try {
            resource.buyStock("AAPL", 10);
            org.junit.Assert.fail("应当抛出 TimeoutException");
        } catch (java.util.concurrent.TimeoutException e) {
            // 期望抛出
        }
    }

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
                resource.buyStock("AAPL", 10);
                double balance = resource.getBalance();
                resource.sellStock("AAPL", 5);
                int position = resource.getPosition("AAPL");
                stat.put("result", "success");
                stat.put("balance", balance);
                stat.put("position", position);
                isSuccess = true;
                System.out.println("[Test] Balance: " + balance + ", Position: " + position);

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
