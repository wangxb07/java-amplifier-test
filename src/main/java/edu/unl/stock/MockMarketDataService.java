package edu.unl.stock;

import java.io.IOException;

import java.util.Random;

public class MockMarketDataService implements MarketDataService {
    private final Random random = new Random();

    @Override
    public double getRealtimePrice(String symbol) throws IOException {
        // 根据股票代码模拟不同的异常情况
        if (symbol.contains("timeout")) {
            throw new IOException("请求超时");
        } else if (symbol.contains("502")) {
            throw new IOException("HTTP 502 Bad Gateway");
        } else if (symbol.contains("disconnect")) {
            throw new IOException("连接断开");
        }
        
        // 返回固定价格，保证测试稳定
        return 100.0;
    }
}
