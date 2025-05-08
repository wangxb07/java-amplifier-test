package edu.unl.exceptionamplifier.resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import edu.unl.exceptionamplifier.mocker.ResourceMocker;
import edu.unl.exceptionamplifier.resource.RemoteApiException;
import edu.unl.exceptionamplifier.resource.InsufficientBalanceException;
import edu.unl.exceptionamplifier.resource.PositionNotEnoughException;

/**
 * 一个相对复杂的被测资源类，模拟股票交易系统的伪代码。
 */
public class StockTradingResource {
    private Map<String, Integer> portfolio = new HashMap<>();
    private double balance;
    private HttpApiCaller httpApiCaller;

    public StockTradingResource(double initialBalance) {
        this(initialBalance, new DefaultHttpApiCaller());
    }

    public StockTradingResource(double initialBalance, HttpApiCaller httpApiCaller) {
        this.balance = initialBalance;
        this.httpApiCaller = httpApiCaller;
    }

    /**
     * 模拟买入股票
     */
    public void buyStock(String symbol, int quantity, double price) throws IOException, RemoteApiException, InsufficientBalanceException {
        if (quantity <= 0 || price <= 0) throw new IllegalArgumentException("数量和价格必须大于0");
        double cost = quantity * price;
        if (balance < cost) throw new InsufficientBalanceException("余额不足，当前余额: " + balance + ", 需要: " + cost);
        // 真实模拟远程API调用
        mockApiCall("StockExchangeApi.buy");
        balance -= cost;
        portfolio.put(symbol, portfolio.getOrDefault(symbol, 0) + quantity);
    }

    /**
     * 模拟卖出股票
     */
    public void sellStock(String symbol, int quantity, double price) throws IOException, RemoteApiException, PositionNotEnoughException {
        if (!portfolio.containsKey(symbol) || portfolio.get(symbol) < quantity) {
            throw new PositionNotEnoughException("持仓不足，当前持仓: " + portfolio.getOrDefault(symbol, 0) + ", 试图卖出: " + quantity);
        }
        // 真实模拟远程API调用
        mockApiCall("StockExchangeApi.sell");
        portfolio.put(symbol, portfolio.get(symbol) - quantity);
        balance += quantity * price;
    }

    /**
     * 查询当前余额
     */
    public double getBalance() throws IOException, RemoteApiException {
        // 真实模拟数据库API调用
        mockApiCall("DatabaseApi.getBalance");
        return balance;
    }

    /**
     * 查询持仓
     */
    public int getPosition(String symbol) throws IOException, RemoteApiException {
        // 真实模拟数据库API调用
        mockApiCall("DatabaseApi.getPosition");
        return portfolio.getOrDefault(symbol, 0);
    }

    /**
     * 伪API调用点，便于Mock异常注入
     */
    // 在每个需要模拟异常的 API 调用前调用此方法
    private void mockApiCall(String apiCall) throws RemoteApiException {
        // 委托给注入的 httpApiCaller
        httpApiCaller.call(apiCall);
        // 保留原有mock逻辑，便于测试异常注入
        new ResourceMocker().mockResourceException(apiCall, "自动注入的异常类型");
    }
}
