package edu.unl.order;

import java.io.IOException;
import java.sql.SQLException;

public class OrderManagementService {
    private final OrderRepository repository;
    private final ProductPriceService priceService;

    public OrderManagementService(OrderRepository repository, ProductPriceService priceService) {
        this.repository = repository;
        this.priceService = priceService;
    }

    public int placeOrder(String productId, int quantity) throws IOException, SQLException, InsufficientBalanceException, InventoryNotEnoughException, RemoteApiException {
        if (productId == null || productId.isEmpty()) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }

        double price = priceService.getPrice(productId);

        int inventory = repository.getInventory(productId);
        if (inventory < quantity) {
            throw new InventoryNotEnoughException("库存不足: " + inventory);
        }

        double balance = repository.getBalance();
        double cost = price * quantity;
        if (balance < cost) {
            throw new InsufficientBalanceException("余额不足: " + balance);
        }

        return repository.placeOrder(productId, quantity, price);
    }

    public void cancelOrder(int orderId) throws SQLException {
        repository.cancelOrder(orderId);
    }

    public double getBalance() throws SQLException {
        return repository.getBalance();
    }

    public int getInventory(String productId) throws SQLException {
        return repository.getInventory(productId);
    }

    public String getOrderStatus(int orderId) throws SQLException {
        return repository.getOrderStatus(orderId);
    }
}
