package edu.unl.exceptionamplifier.explorer;

import edu.unl.exceptionamplifier.mocker.ResourceMocker;
import java.util.*;

public class TestExplorer {
    private final ResourceMocker mocker = new ResourceMocker();
    private final Map<String, Set<String>> exceptionDependencies = new HashMap<>();
    private final Map<String, Integer> executionCount = new HashMap<>();
    private int maxExecutions = 100; // 最大执行次数限制

    public TestExplorer() {
        initializeExceptionDependencies();
    }

    private void initializeExceptionDependencies() {
        // IO相关异常组
        Set<String> ioDeps = new HashSet<>(Arrays.asList(
            "FileNotFoundException",
            "IOException",
            "EOFException",
            "FileSystemException",
            "SecurityException"
        ));

        // 网络相关异常组
        Set<String> networkDeps = new HashSet<>(Arrays.asList(
            "TimeoutException",
            "RemoteApiException",
            "ConnectException",
            "SocketTimeoutException",
            "UnknownHostException",
            "SSLException"
        ));

        // 业务逻辑异常组
        Set<String> businessDeps = new HashSet<>(Arrays.asList(
            "InsufficientBalanceException",
            "PositionNotEnoughException",
            "InvalidOrderException",
            "MarketClosedException",
            "PriceLimitException"
        ));

        // 数据库相关异常组
        Set<String> dbDeps = new HashSet<>(Arrays.asList(
            "SQLException",
            "DataAccessException",
            "TransactionException",
            "DeadlockException",
            "ConnectionException"
        ));

        // 并发相关异常组
        Set<String> concurrentDeps = new HashSet<>(Arrays.asList(
            "ConcurrentModificationException",
            "InterruptedException",
            "ExecutionException",
            "CancellationException",
            "RejectedExecutionException"
        ));

        // 配置相关异常组
        Set<String> configDeps = new HashSet<>(Arrays.asList(
            "ConfigurationException",
            "IllegalArgumentException",
            "IllegalStateException",
            "NullPointerException"
        ));

        // 注册异常依赖关系
        // IO异常组
        exceptionDependencies.put("IOException", ioDeps);
        exceptionDependencies.put("FileNotFoundException", ioDeps);
        exceptionDependencies.put("EOFException", ioDeps);
        exceptionDependencies.put("FileSystemException", ioDeps);

        // 网络异常组
        exceptionDependencies.put("TimeoutException", networkDeps);
        exceptionDependencies.put("RemoteApiException", networkDeps);
        exceptionDependencies.put("ConnectException", networkDeps);
        exceptionDependencies.put("SocketTimeoutException", networkDeps);
        exceptionDependencies.put("UnknownHostException", networkDeps);
        exceptionDependencies.put("SSLException", networkDeps);

        // 业务异常组
        exceptionDependencies.put("InsufficientBalanceException", businessDeps);
        exceptionDependencies.put("PositionNotEnoughException", businessDeps);
        exceptionDependencies.put("InvalidOrderException", businessDeps);
        exceptionDependencies.put("MarketClosedException", businessDeps);
        exceptionDependencies.put("PriceLimitException", businessDeps);

        // 数据库异常组
        exceptionDependencies.put("SQLException", dbDeps);
        exceptionDependencies.put("DataAccessException", dbDeps);
        exceptionDependencies.put("TransactionException", dbDeps);
        exceptionDependencies.put("DeadlockException", dbDeps);
        exceptionDependencies.put("ConnectionException", dbDeps);

        // 并发异常组
        exceptionDependencies.put("ConcurrentModificationException", concurrentDeps);
        exceptionDependencies.put("InterruptedException", concurrentDeps);
        exceptionDependencies.put("ExecutionException", concurrentDeps);
        exceptionDependencies.put("CancellationException", concurrentDeps);
        exceptionDependencies.put("RejectedExecutionException", concurrentDeps);

        // 配置异常组
        exceptionDependencies.put("ConfigurationException", configDeps);
        exceptionDependencies.put("IllegalArgumentException", configDeps);
        exceptionDependencies.put("IllegalStateException", configDeps);
        exceptionDependencies.put("NullPointerException", configDeps);

        // 添加跨组依赖关系
        addCrossGroupDependencies();
    }

    /**
     * 添加跨组异常依赖关系
     */
    private void addCrossGroupDependencies() {
        // 网络异常可能引发IO异常
        Set<String> networkToIO = new HashSet<>(Arrays.asList(
            "IOException",
            "FileSystemException"
        ));
        exceptionDependencies.get("RemoteApiException").addAll(networkToIO);
        exceptionDependencies.get("ConnectException").addAll(networkToIO);

        // 数据库异常可能引发业务异常
        Set<String> dbToBusiness = new HashSet<>(Arrays.asList(
            "InvalidOrderException",
            "PositionNotEnoughException"
        ));
        exceptionDependencies.get("SQLException").addAll(dbToBusiness);
        exceptionDependencies.get("TransactionException").addAll(dbToBusiness);

        // 并发异常可能引发业务异常
        Set<String> concurrentToBusiness = new HashSet<>(Arrays.asList(
            "InsufficientBalanceException",
            "PositionNotEnoughException"
        ));
        exceptionDependencies.get("ConcurrentModificationException").addAll(concurrentToBusiness);
        exceptionDependencies.get("ExecutionException").addAll(concurrentToBusiness);

        // 配置异常可能引发业务异常
        Set<String> configToBusiness = new HashSet<>(Arrays.asList(
            "InvalidOrderException",
            "MarketClosedException"
        ));
        exceptionDependencies.get("ConfigurationException").addAll(configToBusiness);
        exceptionDependencies.get("IllegalStateException").addAll(configToBusiness);
    }

    public void setMaxExecutions(int max) {
        this.maxExecutions = max;
    }

    /**
     * 分析异常依赖关系
     */
    private void analyzeExceptionDependencies(List<String> resources) {
        for (String resource : resources) {
            if (!executionCount.containsKey(resource)) {
                executionCount.put(resource, 0);
            }
        }
    }

    /**
     * 生成有意义的异常组合
     */
    private List<List<String>> generateMeaningfulPatterns(List<List<String>> patterns) {
        List<List<String>> meaningfulPatterns = new ArrayList<>();
        Set<String> processedCombinations = new HashSet<>();

        for (List<String> pattern : patterns) {
            String patternKey = String.join(",", pattern);
            if (processedCombinations.contains(patternKey)) {
                continue;
            }

            // 检查是否是有效的异常组合
            if (isValidExceptionCombination(pattern)) {
                meaningfulPatterns.add(pattern);
                processedCombinations.add(patternKey);
            }
        }

        return meaningfulPatterns;
    }

    /**
     * 检查异常组合是否有效
     */
    private boolean isValidExceptionCombination(List<String> pattern) {
        for (int i = 0; i < pattern.size(); i++) {
            String ex1 = pattern.get(i);
            if ("normal".equals(ex1)) continue;

            for (int j = i + 1; j < pattern.size(); j++) {
                String ex2 = pattern.get(j);
                if ("normal".equals(ex2)) continue;

                // 检查是否存在依赖关系
                if (exceptionDependencies.containsKey(ex1) && 
                    exceptionDependencies.get(ex1).contains(ex2)) {
                    return false; // 避免依赖的异常同时出现
                }
            }
        }
        return true;
    }

    /**
     * 执行测试用例
     */
    private void executeTest(List<String> pattern, ThrowingConsumer<List<String>> testLogic) {
        // 检查执行次数限制
        if (executionCount.values().stream().mapToInt(Integer::intValue).sum() >= maxExecutions) {
            return;
        }

        try {
            // 配置Mocker
            for (int i = 0; i < pattern.size(); i++) {
                String ex = pattern.get(i);
                if (!"normal".equals(ex)) {
                    mocker.mockResourceException("resource_" + i, ex);
                    executionCount.merge("resource_" + i, 1, Integer::sum);
                }
            }

            // 执行测试逻辑
            testLogic.accept(pattern);
            // System.out.println("[TestExplorer] Test finished without uncaught exception.");
        } catch (Exception e) { // Catching Exception from testLogic.accept()
            System.out.println("[TestExplorer] Caught exception during testLogic execution: " + e.getMessage());
            // Optionally re-throw or handle more specifically
        } catch (Throwable t) {
            System.out.println("[TestExplorer] Error: " + t);
        }
    }

    /**
     * 使用智能异常组合进行测试
     */
    public void exploreWithDependencies(List<String> resources, 
                                      List<List<String>> patterns,
                                      ThrowingConsumer<List<String>> testLogic) {
        // 分析异常依赖关系
        analyzeExceptionDependencies(resources);

        // 生成有意义的异常组合
        List<List<String>> meaningfulPatterns = generateMeaningfulPatterns(patterns);

        // 执行测试
        for (List<String> pattern : meaningfulPatterns) {
            executeTest(pattern, testLogic);
        }
    }

    // 保持原有方法以兼容现有代码
    public void explore(List<String> resources, List<String> exceptions) {
        List<List<String>> patterns = new ArrayList<>();
        for (String resource : resources) {
            for (String exception : exceptions) {
                List<String> pattern = new ArrayList<>();
                pattern.add(exception);
                patterns.add(pattern);
            }
        }
        exploreWithDependencies(resources, patterns, (currentPattern) -> {
            // 默认测试逻辑: Do nothing or log, as it now can throw Exception
            System.out.println("[TestExplorer] Default test logic executed for pattern: " + currentPattern);
        });
    }

    public void explore(List<String> resources, 
                       List<List<String>> patterns, 
                       ThrowingConsumer<List<String>> testLogic) {
        exploreWithDependencies(resources, patterns, testLogic);
    }
}
