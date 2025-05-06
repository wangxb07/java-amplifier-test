package edu.unl.exceptionamplifier.testcases;

import edu.unl.exceptionamplifier.resource.StockTradingResource;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class StockTradingResourceTest {
    @Test
    public void testBuyAndSellStock() throws IOException {
        StockTradingResource resource = new StockTradingResource(10000.0);
        resource.buyStock("AAPL", 10, 100.0);
        assertEquals(9000.0, resource.getBalance(), 0.01);
        assertEquals(10, resource.getPosition("AAPL"));

        resource.sellStock("AAPL", 5, 120.0);
        assertEquals(9600.0, resource.getBalance(), 0.01);
        assertEquals(5, resource.getPosition("AAPL"));
    }

    @Test(expected = IOException.class)
    public void testBuyStock_InsufficientBalance() throws IOException {
        StockTradingResource resource = new StockTradingResource(100.0);
        resource.buyStock("GOOG", 10, 100.0); // 总价1000，余额不足，应抛出IOException
    }

    @Test(expected = IOException.class)
    public void testSellStock_InsufficientPosition() throws IOException {
        StockTradingResource resource = new StockTradingResource(10000.0);
        resource.buyStock("TSLA", 1, 500.0);
        resource.sellStock("TSLA", 2, 600.0); // 持仓不足，应抛出IOException
    }
}
