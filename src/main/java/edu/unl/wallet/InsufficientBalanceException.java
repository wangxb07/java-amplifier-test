package edu.unl.wallet;

public class InsufficientBalanceException extends Exception {
    public InsufficientBalanceException(String message) { super(message); }
}
