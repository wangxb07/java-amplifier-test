package edu.unl.stock;

import java.io.IOException;

import java.util.Random;

public class MockMarketDataService implements MarketDataService {
    private final Random random = new Random();

    @Override
    public double getRealtimePrice(String symbol) throws IOException {
        // 模拟价格：10~100元之间浮动
        return 10 + random.nextDouble() * 90;
    }
}
