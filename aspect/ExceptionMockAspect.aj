package edu.unl.exceptionamplifier.aspect;

public aspect ExceptionMockAspect {
    // Pointcut for mocking resource exceptions
    pointcut mockResource(String resource, String exceptionType):
        call(void edu.unl.exceptionamplifier.mocker.ResourceMocker.mockResourceException(String, String)) &&
        args(resource, exceptionType);

    void around(String resource, String exceptionType): mockResource(resource, exceptionType) {
        // Throw the configured exception type
        System.out.println("[AspectJ] Mocked exception: " + exceptionType + " for resource: " + resource);
        if (!"normal".equals(exceptionType)) {
            if ("IOException".equals(exceptionType)) {
                throw new java.io.IOException("Mocked IOException by AspectJ");
            } else if ("TimeoutException".equals(exceptionType)) {
                throw new java.util.concurrent.TimeoutException("Mocked TimeoutException by AspectJ");
            } else {
                throw new RuntimeException(exceptionType + " (mocked by AspectJ)");
            }
        }
    }
}
