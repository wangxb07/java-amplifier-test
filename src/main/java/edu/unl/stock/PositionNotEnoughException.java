package edu.unl.stock;

public class PositionNotEnoughException extends Exception {
    public PositionNotEnoughException(String message) { super(message); }
}
