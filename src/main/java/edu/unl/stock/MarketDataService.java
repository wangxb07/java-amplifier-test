package edu.unl.stock;

import java.io.IOException;

public interface MarketDataService {
    /**
     * 获取指定股票代码的实时价格
     */
    double getRealtimePrice(String symbol) throws IOException, RemoteApiException;
}
