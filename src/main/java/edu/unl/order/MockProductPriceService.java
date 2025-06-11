package edu.unl.order;

import java.io.IOException;
import java.util.Random;

/**
 * 简易的价格服务实现，根据商品ID模拟不同异常或返回固定价格。
 */
public class MockProductPriceService implements ProductPriceService {
    private final Random random = new Random();

    @Override
    public double getPrice(String productId) throws IOException {
        if (productId.contains("timeout")) {
            throw new IOException("请求超时");
        } else if (productId.contains("502")) {
            throw new IOException("HTTP 502 Bad Gateway");
        } else if (productId.contains("disconnect")) {
            throw new IOException("连接断开");
        }

        return 50.0; // 固定价格以保证测试稳定
    }
}
