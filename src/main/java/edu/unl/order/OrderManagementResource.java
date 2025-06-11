package edu.unl.order;

import java.io.IOException;
import java.sql.SQLException;

/**
 * 订单管理资源类，供测试使用。
 */
public class OrderManagementResource {
    private final OrderManagementService service;

    public OrderManagementResource(OrderManagementService service) {
        this.service = service;
    }

    public OrderManagementResource(double initialBalance) {
        OrderRepository repository = new OrderRepository();
        repository.initDatabase(initialBalance);
        ProductPriceService priceService = new MockProductPriceService();
        this.service = new OrderManagementService(repository, priceService);
    }

    public int placeOrder(String productId, int quantity) throws IOException, SQLException, InsufficientBalanceException, InventoryNotEnoughException, RemoteApiException {
        return service.placeOrder(productId, quantity);
    }

    public void cancelOrder(int orderId) throws SQLException {
        service.cancelOrder(orderId);
    }

    public double getBalance() throws SQLException {
        return service.getBalance();
    }

    public int getInventory(String productId) throws SQLException {
        return service.getInventory(productId);
    }

    public String getOrderStatus(int orderId) throws SQLException {
        return service.getOrderStatus(orderId);
    }
}
