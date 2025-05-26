package edu.unl.exceptionamplifier.explorer;

@FunctionalInterface
public interface ThrowingConsumer<T> {
    void accept(T t) throws Exception;
}
