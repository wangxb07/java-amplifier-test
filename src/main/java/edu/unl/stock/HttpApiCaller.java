package edu.unl.stock;

public interface HttpApiCaller {
    /**
     * 调用远程API，抛出异常模拟网络问题
     * @param apiCall API名称
     * @throws RemoteApiException 网络相关异常
     */
    void call(String apiCall) throws RemoteApiException;
}
