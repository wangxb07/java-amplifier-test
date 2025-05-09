package edu.unl.stock;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class DefaultHttpApiCaller implements HttpApiCaller {
    @Override
    public void call(String apiCall) throws RemoteApiException {
        try {
            // 根据不同的API调用抛出相应的异常
            if (apiCall.contains("502")) {
                throw new RemoteApiException("远程API调用失败: HTTP 502 Bad Gateway");
            } else if (apiCall.contains("disconnect")) {
                throw new RemoteApiException("远程API调用失败: 连接断开");
            } else if (apiCall.contains("timeout")) {
                throw new RemoteApiException("远程API调用失败: 请求超时");
            }

            // 假设远程API地址如下（仅为演示）
            URL url = new URL("http://api.example.com/mock?api=" + apiCall);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            // 简单模拟：非200都认为失败
            if (code != 200) {
                throw new RemoteApiException("远程API调用失败: " + apiCall + ", 响应码: " + code);
            }
            // 也可以随机抛出异常，模拟网络波动
            if (new Random().nextInt(10) == 0) {
                throw new RemoteApiException("远程API调用随机故障: " + apiCall);
            }
        } catch (RemoteApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteApiException("远程API调用异常: " + apiCall, e);
        }
    }
}
