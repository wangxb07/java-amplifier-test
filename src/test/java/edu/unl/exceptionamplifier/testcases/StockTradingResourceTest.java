package edu.unl.exceptionamplifier.testcases;

import edu.unl.stock.StockTradingResource;

import edu.unl.stock.InsufficientBalanceException;
import edu.unl.stock.PositionNotEnoughException;
import org.junit.Test;

import static org.junit.Assert.*;

public class StockTradingResourceTest {
    @Test
    public void testBuyAndSellStock() {
        try {
            StockTradingResource resource = new StockTradingResource(10000.0);
            resource.buyStock("AAPL", 10);
            assertEquals(9000.0, resource.getBalance(), 0.01);
            assertEquals(10, resource.getPosition("AAPL"));

            resource.sellStock("AAPL", 5);
            assertEquals(9600.0, resource.getBalance(), 0.01);
            assertEquals(5, resource.getPosition("AAPL"));
        
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
            resource.buyStock("GOOG", 10);
            fail("应当抛出 InsufficientBalanceException");
        } catch (InsufficientBalanceException e) {
            // 预期异常
        
        } catch (Exception e) {
            fail("未预期的异常: " + e.getMessage());
        }
    }

    @Test
    public void testSellStock_InsufficientPosition() {
        try {
            StockTradingResource resource = new StockTradingResource(10000.0);
            resource.buyStock("TSLA", 1);
            resource.sellStock("TSLA", 2);
            fail("应当抛出 PositionNotEnoughException");
        } catch (PositionNotEnoughException e) {
            // 预期异常
        
        } catch (InsufficientBalanceException e) {
            fail("不应抛出余额不足异常: " + e.getMessage());
        } catch (Exception e) {
            fail("未预期的异常: " + e.getMessage());
        }
    }

    @Test
    public void testBuyStock_HttpTimeout() {
        StockTradingResource resource = new StockTradingResource(10000.0);
        try {
            resource.buyStock("AAPL", 1);
            fail("应当抛出异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("超时"));
        }
    }

    @Test
    public void testBuyStock_Http502() {
        StockTradingResource resource = new StockTradingResource(10000.0);
        try {
            resource.buyStock("AAPL", 1);
            fail("应当抛出异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("502"));
        }
    }

    @Test
    public void testBuyStock_HttpDisconnect() {
        StockTradingResource resource = new StockTradingResource(10000.0);
        try {
            resource.buyStock("AAPL", 1);
            fail("应当抛出异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("断开"));
        }
    }

    @Test
    public void testBuyStock_HttpSuccess() {
        StockTradingResource resource = new StockTradingResource(10000.0);
        try {
            resource.buyStock("AAPL", 1);
            // 不抛异常即为成功
        } catch (Exception e) {
            fail("不应抛出异常: " + e.getMessage());
        }
    }
}
