package edu.unl.order;

public class RemoteApiException extends Exception {
    public RemoteApiException(String message) { super(message); }
    public RemoteApiException(String message, Throwable cause) { super(message, cause); }
}
