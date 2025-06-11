package edu.unl.order;

public class InventoryNotEnoughException extends Exception {
    public InventoryNotEnoughException(String message) { super(message); }
}
