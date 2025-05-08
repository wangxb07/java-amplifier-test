package edu.unl.stock;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import edu.unl.exceptionamplifier.mocker.ResourceMocker;
import edu.unl.stock.RemoteApiException;
import edu.unl.stock.InsufficientBalanceException;
import edu.unl.stock.PositionNotEnoughException;

/**
 * 一个相对复杂的被测资源类，模拟股票交易系统的伪代码。
 */
public class StockTradingResource {
    private static final String DB_URL = "jdbc:sqlite:stock_trading.db";
    private HttpApiCaller httpApiCaller;
    private final StockTradingRepository repository;

    public StockTradingResource(double initialBalance) {
        this(initialBalance, new DefaultHttpApiCaller());
    }

    public StockTradingResource(double initialBalance, HttpApiCaller httpApiCaller) {
        this.httpApiCaller = httpApiCaller;
        this.repository = new StockTradingRepository();
        repository.initDatabase(initialBalance);
    }


    /**
     * 买入股票：
     * 1. 校验参数
     * 2. 查询本地余额（模拟DB）
     * 3. 通过API下单买入，若成功则更新本地持仓和余额（模拟DB写入）
     */
    public void buyStock(String symbol, int quantity, double price) throws IOException, RemoteApiException, InsufficientBalanceException {
        if (symbol == null || symbol.isEmpty()) throw new IllegalArgumentException("股票代码不能为空");
        if (quantity <= 0 || price <= 0) throw new IllegalArgumentException("数量和价格必须大于0");
        double cost = quantity * price;
        double balance = getBalance();
        if (balance < cost) throw new InsufficientBalanceException("余额不足，当前余额: " + balance + ", 需要: " + cost);
        // 远程API下单买入
        boolean apiSuccess = placeOrderApi(symbol, quantity, price, "buy");
        if (!apiSuccess) throw new RemoteApiException("远程买入API失败");
        // 更新数据库余额和持仓，并插入交易流水
        repository.updateBalance(-cost);
        repository.updatePosition(symbol, quantity);
        repository.insertTrade(symbol, quantity, price, "buy");
    }

    /**
     * 卖出股票：
     * 1. 校验参数
     * 2. 查询本地持仓（模拟DB）
     * 3. 通过API下单卖出，若成功则更新本地持仓和余额（模拟DB写入）
     */
    public void sellStock(String symbol, int quantity, double price) throws IOException, RemoteApiException, PositionNotEnoughException {
        if (symbol == null || symbol.isEmpty()) throw new IllegalArgumentException("股票代码不能为空");
        if (quantity <= 0 || price <= 0) throw new IllegalArgumentException("数量和价格必须大于0");
        int current = getPosition(symbol);
        if (current < quantity) throw new PositionNotEnoughException("持仓不足，当前持仓: " + current + ", 试图卖出: " + quantity);
        // 远程API下单卖出
        boolean apiSuccess = placeOrderApi(symbol, quantity, price, "sell");
        if (!apiSuccess) throw new RemoteApiException("远程卖出API失败");
        // 更新数据库余额和持仓，并插入交易流水
        repository.updateBalance(quantity * price);
        repository.updatePosition(symbol, -quantity);
        repository.insertTrade(symbol, quantity, price, "sell");
    }

    /**
     * 查询当前余额（模拟数据库查询）
     */
    public double getBalance() throws IOException {
        return repository.getBalance();
    }

    /**
     * 查询持仓（模拟数据库查询）
     */
    public int getPosition(String symbol) throws IOException {
        return repository.getPosition(symbol);
    }

    /**
     * 真实业务：远程API下单（买入/卖出）
     * 返回true表示成功，false表示失败
     */
    private boolean placeOrderApi(String symbol, int quantity, double price, String type) throws RemoteApiException {
        // 真实业务：HTTP请求下单
        String apiUrl = "http://api.stockexchange.local/order";
        java.net.HttpURLConnection connection = null;
        try {
            java.net.URL url = new java.net.URL(apiUrl);
            connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            String body = String.format("{\"symbol\":\"%s\",\"quantity\":%d,\"price\":%.2f,\"type\":\"%s\"}", symbol, quantity, price, type);
            try (java.io.OutputStream os = connection.getOutputStream()) {
                os.write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            int code = connection.getResponseCode();
            if (code == 200) {
                // 可进一步解析返回内容判断业务成功
                return true;
            } else {
                throw new RemoteApiException("下单API返回码: " + code);
            }
        } catch (java.io.IOException e) {
            throw new RemoteApiException("下单HTTP请求异常: " + e.getMessage(), e);
        } finally {
            if (connection != null) connection.disconnect();
        }
        // mockApiCall("StockExchangeApi." + type); // 如需异常注入测试可保留

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
