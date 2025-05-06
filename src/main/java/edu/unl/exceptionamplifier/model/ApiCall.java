package edu.unl.exceptionamplifier.model;

public class ApiCall {
    private String className;
    private String methodName;

    public ApiCall(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }
}
