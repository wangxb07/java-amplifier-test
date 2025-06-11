package edu.unl.order;

public class Order {
    private int id;
    private String productId;
    private int quantity;
    private double price;
    private String status;

    public Order(int id, String productId, int quantity, double price, String status) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }
}
