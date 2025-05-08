package edu.unl.stock;

public class InsufficientBalanceException extends Exception {
    public InsufficientBalanceException(String message) { super(message); }
}
