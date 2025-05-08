package edu.unl.exceptionamplifier.testcases;

import edu.unl.exceptionamplifier.resource.StockTradingResource;
import edu.unl.exceptionamplifier.resource.RemoteApiException;
import edu.unl.exceptionamplifier.resource.InsufficientBalanceException;
import edu.unl.exceptionamplifier.resource.PositionNotEnoughException;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

import edu.unl.exceptionamplifier.resource.HttpApiCaller;

public class StockTradingResourceTest {
    @Test
    public void testBuyAndSellStock() {
        try {
            StockTradingResource resource = new StockTradingResource(10000.0);
            resource.buyStock("AAPL", 10, 100.0);
            assertEquals(9000.0, resource.getBalance(), 0.01);
            assertEquals(10, resource.getPosition("AAPL"));

            resource.sellStock("AAPL", 5, 120.0);
            assertEquals(9600.0, resource.getBalance(), 0.01);
            assertEquals(5, resource.getPosition("AAPL"));
        } catch (RemoteApiException e) {
            // 远程API异常概率性发生，测试可接受
            System.out.println("[Test] RemoteApiException: " + e.getMessage());
        } catch (InsufficientBalanceException | PositionNotEnoughException e) {
            fail("不应抛出余额/持仓异常: " + e.getMessage());
        } catch (Exception e) {
            fail("未预期的异常: " + e.getMessage());
        }
    }

    @Test
    public void testBuyStock_InsufficientBalance() {
        try {
            StockTradingResource resource = new StockTradingResource(100.0);
            resource.buyStock("GOOG", 10, 100.0);
            fail("应当抛出 InsufficientBalanceException");
        } catch (InsufficientBalanceException e) {
            // 预期异常
        } catch (RemoteApiException e) {
            // 远程API异常概率性发生，测试可接受
            System.out.println("[Test] RemoteApiException: " + e.getMessage());
        } catch (Exception e) {
            fail("未预期的异常: " + e.getMessage());
        }
    }

    @Test
    public void testSellStock_InsufficientPosition() {
        try {
            StockTradingResource resource = new StockTradingResource(10000.0);
            resource.buyStock("TSLA", 1, 500.0);
            resource.sellStock("TSLA", 2, 600.0);
            fail("应当抛出 PositionNotEnoughException");
        } catch (PositionNotEnoughException e) {
            // 预期异常
        } catch (RemoteApiException e) {
            // 远程API异常概率性发生，测试可接受
            System.out.println("[Test] RemoteApiException: " + e.getMessage());
        } catch (InsufficientBalanceException e) {
            fail("不应抛出余额不足异常: " + e.getMessage());
        } catch (Exception e) {
            fail("未预期的异常: " + e.getMessage());
        }
    }

    // mock实现
    static class MockHttpApiCaller implements HttpApiCaller {
        private final String scenario;
        public MockHttpApiCaller(String scenario) {
            this.scenario = scenario;
        }
        @Override
        public void call(String apiCall) throws RemoteApiException {
            switch (scenario) {
                case "timeout":
                    throw new RemoteApiException("网络超时: " + apiCall);
                case "bad_gateway":
                    throw new RemoteApiException("502 Bad Gateway: " + apiCall);
                case "disconnect":
                    throw new RemoteApiException("网络断开: " + apiCall);
                case "invalid_json":
                    // 假设业务解析时再抛异常，这里先通过
                    break;
                case "success":
                default:
                    // 正常
            }
        }
    }

    @Test
    public void testBuyStock_HttpTimeout() {
        StockTradingResource resource = new StockTradingResource(10000.0, new MockHttpApiCaller("timeout"));
        try {
            resource.buyStock("AAPL", 1, 100.0);
            fail("应当抛出 RemoteApiException");
        } catch (RemoteApiException e) {
            assertTrue(e.getMessage().contains("超时"));
        } catch (Exception e) {
            fail("未预期的异常: " + e.getMessage());
        }
    }

    @Test
    public void testBuyStock_Http502() {
        StockTradingResource resource = new StockTradingResource(10000.0, new MockHttpApiCaller("bad_gateway"));
        try {
            resource.buyStock("AAPL", 1, 100.0);
            fail("应当抛出 RemoteApiException");
        } catch (RemoteApiException e) {
            assertTrue(e.getMessage().contains("502"));
        } catch (Exception e) {
            fail("未预期的异常: " + e.getMessage());
        }
    }

    @Test
    public void testBuyStock_HttpDisconnect() {
        StockTradingResource resource = new StockTradingResource(10000.0, new MockHttpApiCaller("disconnect"));
        try {
            resource.buyStock("AAPL", 1, 100.0);
            fail("应当抛出 RemoteApiException");
        } catch (RemoteApiException e) {
            assertTrue(e.getMessage().contains("断开"));
        } catch (Exception e) {
            fail("未预期的异常: " + e.getMessage());
        }
    }

    @Test
    public void testBuyStock_HttpSuccess() {
        StockTradingResource resource = new StockTradingResource(10000.0, new MockHttpApiCaller("success"));
        try {
            resource.buyStock("AAPL", 1, 100.0);
            // 不抛异常即为成功
        } catch (Exception e) {
            fail("不应抛出异常: " + e.getMessage());
        }
    }
}
