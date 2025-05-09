package edu.unl.stock;

import java.io.IOException;

import java.util.Random;

public class MockMarketDataService implements MarketDataService {
    private final Random random = new Random();

    @Override
    public double getRealtimePrice(String symbol) throws IOException {
        // 返回固定价格，保证测试稳定
        return 100.0;
    }
}
