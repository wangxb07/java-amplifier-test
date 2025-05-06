package edu.unl.exceptionamplifier.mocker;

public class ResourceMocker {
    public void mockResourceException(String resource, String exceptionType) {
        // Insert mock logic here (to be intercepted by AspectJ)
        System.out.println("Mocking exception: " + exceptionType + " for resource: " + resource);
    }
}
