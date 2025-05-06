package edu.unl.exceptionamplifier.model;

public class MockingPattern {
    private String resource;
    private String exception;

    public MockingPattern(String resource, String exception) {
        this.resource = resource;
        this.exception = exception;
    }

    public String getResource() {
        return resource;
    }

    public String getException() {
        return exception;
    }
}
