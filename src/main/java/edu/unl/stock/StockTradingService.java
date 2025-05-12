package edu.unl.stock;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

public class StockTradingService {
    private final StockTradingRepository repository;
    private final MarketDataService marketDataService;
    private static final double MAX_PRICE = 1000000.0; // 最大价格限制
    private static final int MAX_QUANTITY = 1000000;   // 最大交易数量限制
    private static final double MIN_PRICE = 0.01;      // 最小价格限制

    public StockTradingService(StockTradingRepository repository, MarketDataService marketDataService) {
        this.repository = repository;
        this.marketDataService = marketDataService;
    }

    /**
     * 买入股票，包含业务校验和事务操作
     */
    /**
     * 买入股票，自动获取实时价格
     */
    public void buyStock(String symbol, int quantity) throws IOException, SQLException, TimeoutException, InsufficientBalanceException, RemoteApiException {
        // 基础参数校验
        if (symbol == null || symbol.isEmpty()) {
            throw new IllegalArgumentException("股票代码不能为空");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }
        if (quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("交易数量超过最大限制: " + MAX_QUANTITY);
        }

        // 获取实时价格
        double price = marketDataService.getRealtimePrice(symbol);

        // 价格校验
        if (price < MIN_PRICE) {
            throw new IllegalArgumentException("实时价格过低: " + price);
        }
        if (price > MAX_PRICE) {
            throw new IllegalArgumentException("实时价格过高: " + price);
        }

        // 检查持仓
        int currentPosition;
        try {
            currentPosition = repository.getPosition(symbol);
        } catch (InsufficientBalanceException e) {
            throw new InsufficientBalanceException("获取持仓信息时余额不足: " + e.getMessage());
        }

        // 计算交易金额
        double cost = quantity * price;
        if (cost > MAX_PRICE * MAX_QUANTITY) {
            throw new IllegalArgumentException("交易金额超过最大限制");
        }

        // 检查余额
        double balance;
        try {
            balance = repository.getBalance();
        } catch (InsufficientBalanceException e) {
            throw new InsufficientBalanceException("获取余额信息时余额不足: " + e.getMessage());
        }

        if (balance < cost) {
            throw new InsufficientBalanceException("余额不足，当前余额: " + balance + ", 需要: " + cost);
        }

        // 执行交易
        try {
            repository.executeTradeTransaction(symbol, quantity, price, "buy");
        } catch (SQLException e) {
            throw new SQLException("交易执行失败: " + e.getMessage());
        } catch (TimeoutException e) {
            throw new TimeoutException("交易执行超时: " + e.getMessage());
        }
    }

    /**
     * 卖出股票，包含业务校验和事务操作
     */
    /**
     * 卖出股票，自动获取实时价格
     */
    public void sellStock(String symbol, int quantity) throws IOException, SQLException, TimeoutException, PositionNotEnoughException, InsufficientBalanceException, RemoteApiException {
        // 基础参数校验
        if (symbol == null || symbol.isEmpty()) {
            throw new IllegalArgumentException("股票代码不能为空");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }
        if (quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("交易数量超过最大限制: " + MAX_QUANTITY);
        }

        // 检查持仓
        int currentPosition;
        try {
            currentPosition = repository.getPosition(symbol);
        } catch (InsufficientBalanceException e) {
            // 卖出时忽略余额不足异常
            currentPosition = 0;
        }

        if (currentPosition < quantity) {
            throw new PositionNotEnoughException("持仓不足，当前持仓: " + currentPosition + ", 试图卖出: " + quantity);
        }

        // 获取实时价格
        double price = marketDataService.getRealtimePrice(symbol);

        // 价格校验
        if (price < MIN_PRICE) {
            throw new IllegalArgumentException("实时价格过低: " + price);
        }
        if (price > MAX_PRICE) {
            throw new IllegalArgumentException("实时价格过高: " + price);
        }

        // 执行交易
        try {
            repository.executeTradeTransaction(symbol, quantity, price, "sell");
        } catch (SQLException e) {
            throw new SQLException("交易执行失败: " + e.getMessage());
        } catch (TimeoutException e) {
            throw new TimeoutException("交易执行超时: " + e.getMessage());
        } catch (InsufficientBalanceException e) {
            // 卖出时忽略余额不足异常
        }
    }

    public double getBalance() throws SQLException, TimeoutException, InsufficientBalanceException {
        try {
            return repository.getBalance();
        } catch (SQLException e) {
            throw new SQLException("数据库操作失败: " + e.getMessage());
        } catch (TimeoutException e) {
            throw new TimeoutException("操作超时: " + e.getMessage());
        } catch (InsufficientBalanceException e) {
            throw new InsufficientBalanceException("账户余额不足: " + e.getMessage());
        }
    }

    public int getPosition(String symbol) throws SQLException, TimeoutException, InsufficientBalanceException {
        if (symbol == null || symbol.isEmpty()) {
            throw new IllegalArgumentException("股票代码不能为空");
        }
        try {
            return repository.getPosition(symbol);
        } catch (SQLException e) {
            throw new SQLException("数据库操作失败: " + e.getMessage());
        } catch (TimeoutException e) {
            throw new TimeoutException("操作超时: " + e.getMessage());
        } catch (InsufficientBalanceException e) {
            throw new InsufficientBalanceException("获取持仓信息时余额不足: " + e.getMessage());
        }
    }
}
