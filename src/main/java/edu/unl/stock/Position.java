package edu.unl.stock;

public class Position {
    private String symbol;
    private int quantity;
    private double averagePrice;

    public Position(String symbol, int quantity, double averagePrice) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getAveragePrice() {
        return averagePrice;
    }

    // Add setters if needed, or other relevant methods
}
