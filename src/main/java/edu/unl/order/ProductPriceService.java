package edu.unl.order;

import java.io.IOException;

/**
 * 获取商品价格的服务接口，可通过不同实现模拟网络异常等情况。
 */
public interface ProductPriceService {
    double getPrice(String productId) throws IOException, RemoteApiException;
}
