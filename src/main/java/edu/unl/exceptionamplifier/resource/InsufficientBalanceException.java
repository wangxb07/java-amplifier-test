package edu.unl.exceptionamplifier.resource;

public class InsufficientBalanceException extends Exception {
    public InsufficientBalanceException(String message) { super(message); }
}
