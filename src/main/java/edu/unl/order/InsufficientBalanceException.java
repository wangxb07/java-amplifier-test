package edu.unl.order;

public class InsufficientBalanceException extends Exception {
    public InsufficientBalanceException(String message) { super(message); }
}
