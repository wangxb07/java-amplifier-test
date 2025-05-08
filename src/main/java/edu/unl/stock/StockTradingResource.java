package edu.unl.stock;

import java.io.IOException;
import java.sql.SQLException;

/**
 * 一个相对复杂的被测资源类，模拟股票交易系统的伪代码。
 */
public class StockTradingResource {
    private final StockTradingService service;

    // 新增：允许注入自定义service，便于单元测试和mock
    public StockTradingResource(StockTradingService service) {
        this.service = service;
    }

    public StockTradingResource(double initialBalance) {
        StockTradingRepository repository = new StockTradingRepository();
        repository.initDatabase(initialBalance);
        MarketDataService marketDataService = new MockMarketDataService();
        this.service = new StockTradingService(repository, marketDataService);
    }

    /**
     * 买入股票：
     * 1. 校验参数
     * 2. 查询本地余额（模拟DB）
     * 3. 通过API下单买入，若成功则更新本地持仓和余额（模拟DB写入）
     */
    public void buyStock(String symbol, int quantity) throws IOException, SQLException, java.util.concurrent.TimeoutException, InsufficientBalanceException {
        // 直接调用业务逻辑，省略API下单
        service.buyStock(symbol, quantity);
    }

    /**
     * 卖出股票：
     * 1. 校验参数
     * 2. 查询本地持仓（模拟DB）
     * 3. 通过API下单卖出，若成功则更新本地持仓和余额（模拟DB写入）
     */
    public void sellStock(String symbol, int quantity) throws IOException, SQLException, java.util.concurrent.TimeoutException, PositionNotEnoughException, InsufficientBalanceException {
        // 直接调用业务逻辑，省略API下单
        service.sellStock(symbol, quantity);
    }

    /**
     * 查询当前余额（模拟数据库查询）
     */
    public double getBalance() throws IOException, SQLException, java.util.concurrent.TimeoutException, InsufficientBalanceException {
        return service.getBalance();
    }

    /**
     * 查询持仓（模拟数据库查询）
     */
    public int getPosition(String symbol) throws IOException, SQLException, java.util.concurrent.TimeoutException, InsufficientBalanceException {
        return service.getPosition(symbol);
    }




}
