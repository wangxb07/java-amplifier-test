package edu.unl.exceptionamplifier.explorer;

import edu.unl.exceptionamplifier.mocker.ResourceMocker;
import java.util.List;

public class TestExplorer {
    private final ResourceMocker mocker = new ResourceMocker();

    public void explore(List<String> resources, List<String> exceptions) {
        for (String resource : resources) {
            for (String exception : exceptions) {
                mocker.mockResourceException(resource, exception);
                // Execute test logic here
            }
        }
    }

    /**
     * 遍历所有资源-异常组合并执行回放，检测异常情况
     * @param resources 资源API调用名列表（如 FileInputStream）
     * @param patterns 每种模式对应每个资源的异常类型或normal
     * @param runnable 测试用例回放逻辑（通常为lambda表达式）
     */
    public void explore(List<String> resources, List<List<String>> patterns, Runnable runnable) {
        for (List<String> pattern : patterns) {
            System.out.println("[TestExplorer] Pattern: " + pattern);
            try {
                // 配置Mocker（这里假设mocker可按pattern设置）
                for (int i = 0; i < resources.size(); i++) {
                    String resource = resources.get(i);
                    String ex = pattern.get(i);
                    if (!"normal".equals(ex)) {
                        mocker.mockResourceException(resource, ex);
                    }
                }
                runnable.run();
                System.out.println("[TestExplorer] Test finished without uncaught exception.");
            } catch (Exception e) {
                System.out.println("[TestExplorer] Caught exception: " + e);
            } catch (Throwable t) {
                System.out.println("[TestExplorer] Error: " + t);
            }
        }
    }
}
