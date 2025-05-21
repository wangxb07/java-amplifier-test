package edu.unl.exceptionamplifier.testcases;

import edu.unl.stock.StockTradingResource;
import edu.unl.stock.RemoteApiException;
import edu.unl.stock.MockMarketDataService;
import edu.unl.exceptionamplifier.builder.ExceptionalSpaceBuilder;
import edu.unl.exceptionamplifier.util.CoverageStatsReporter;
import edu.unl.exceptionamplifier.util.CoverageStatsReporter.ExceptionDetails;
import edu.unl.exceptionamplifier.explorer.TestExplorer;
import edu.unl.exceptionamplifier.util.ExceptionReflectionUtils;

import org.junit.Test;
import org.junit.AfterClass;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import edu.unl.stock.StockTradingRepository;
import edu.unl.stock.MarketDataService;
import java.io.IOException;
import java.sql.SQLException;

import java.util.Arrays;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors; // Added for Collectors.toList()

import java.lang.reflect.Constructor;

public class StockTradingResourceAmplifiedTest {
    // private static final CoverageStatsReporter reporter = new CoverageStatsReporter(); // Commented out as it's not used by the amplified test logic directly for adding stats.
    private static final Set<String> requiredExceptions = new HashSet<>();
    private static final Set<String> coveredExceptions = new HashSet<>(); // This set is updated in the explorer lambda

    static {
        // 自动收集 StockTradingResource 和 StockTradingService 的所有 public 方法声明的异常类型
        Class<?>[] classes = {edu.unl.stock.StockTradingResource.class, edu.unl.stock.StockTradingService.class};
        for (Class<?> clazz : classes) {
            for (Method method : clazz.getMethods()) {
                for (Class<?> ex : method.getExceptionTypes()) {
                    requiredExceptions.add(ex.getSimpleName());
                }
            }
        }
        // 移除常见的 RuntimeException/Exception/Object（只保留业务相关异常）
        requiredExceptions.remove("Exception");
        requiredExceptions.remove("RuntimeException");
        requiredExceptions.remove("Throwable");
        requiredExceptions.remove("Object");
        // 移除不相关的 InterruptedException
        requiredExceptions.remove("InterruptedException");
    }

    @AfterClass
    public static void printExceptionCoverage() {
        System.out.println("\n[需要覆盖的异常类型] " + requiredExceptions);
        System.out.println("[已通过放大测试覆盖的异常类型] " + coveredExceptions);
        int total = requiredExceptions.size();
        int covered = 0;
        for (String ex : requiredExceptions) {
            if (coveredExceptions.contains(ex)) covered++;
        }
        double rate = total == 0 ? 1.0 : (covered * 1.0 / total);
        System.out.printf("[异常覆盖率] %d/%d = %.2f%%\n", covered, total, rate * 100);
    }

    // Individual @Test methods are removed as their functionality is covered by testAmplifiedBuyAndSell.

    @Test
    public void testAmplifiedBuyAndSell() {
        // 1. Define the sequence of potentially exception-throwing dependency calls
        List<String> apiCalls = Arrays.asList(
                "MarketDataService.getRealtimePrice", // During buy
                "StockTradingRepository.executeTradeTransaction", // During buy
                "MarketDataService.getRealtimePrice", // During sell
                "StockTradingRepository.executeTradeTransaction"  // During sell
        );

        // ---- START INTEGRATION OF EXCEPTION COUNTING ----
        Map<String, String> fqcnMap = new HashMap<>();
        fqcnMap.put("MarketDataService", "edu.unl.stock.MarketDataService");
        fqcnMap.put("StockTradingRepository", "edu.unl.stock.StockTradingRepository");
        // Add other FQCNs if your sequence includes other service keys from apiCalls

        int totalDeclaredExceptionsInPath = 
            ExceptionReflectionUtils.countDeclaredExceptions(apiCalls, fqcnMap);

        System.out.println("--- Test: testAmplifiedBuyAndSell ---");
        System.out.println("API Call Sequence for amplified buy/sell (potential path): " + apiCalls);
        System.out.println("Total potential declared exception points in this path: " + totalDeclaredExceptionsInPath);
        // ---- END INTEGRATION OF EXCEPTION COUNTING ----

        // 2. Define exception types to inject (ensure SQLException is included if repository throws it)
        List<String> exceptionTypes = Arrays.asList(
            "IOException", "SQLException",
            "InsufficientBalanceException", "PositionNotEnoughException", "RemoteApiException",
            "IllegalArgumentException" // Include validation exceptions if desired
        );

        // 3. Generate all mocking patterns
        ExceptionalSpaceBuilder builder = new ExceptionalSpaceBuilder();
        List<List<String>> patterns = builder.generateMockingPatterns(apiCalls, exceptionTypes, false); // includeNormal=true

        // 4. Use TestExplorer to explore each pattern
        CoverageStatsReporter coverageStats = new CoverageStatsReporter(); // Local reporter for path coverage
        TestExplorer explorer = new TestExplorer();

        System.out.println("[TestExplorer] Starting exploration of " + patterns.size() + " patterns...");

        explorer.explore(apiCalls, patterns, (pattern) -> {
            long startTime = System.currentTimeMillis();
            Map<String, Object> stat = new HashMap<>();
            stat.put("pattern", pattern);
            stat.put("apiCalls", apiCalls.toString());
            boolean isSuccess = false;

            // --- Dynamic Mocking Setup ---
            MarketDataService mockMarketData = mock(MarketDataService.class);
            StockTradingRepository mockRepo = mock(StockTradingRepository.class);

            try {
                // Default "normal" behavior
                when(mockMarketData.getRealtimePrice(anyString())).thenReturn(100.0); // Normal price
                when(mockRepo.getBalance()).thenReturn(10000.0); // Sufficient balance initially
                when(mockRepo.getPosition(anyString())).thenReturn(0); // No initial position for buy, assume 10 for sell check below
                // Need to handle executeTradeTransaction carefully as it doesn't return a value
                // Use lenient() for methods that might not be called in every path
                lenient().doNothing().when(mockRepo).executeTradeTransaction(anyString(), anyInt(), anyDouble(), anyString());

                // Apply exceptions based on the current pattern
                for (int i = 0; i < pattern.size(); i++) {
                    String exceptionName = pattern.get(i);
                    if (!"normal".equalsIgnoreCase(exceptionName)) {
                        String apiCall = apiCalls.get(i);
                        Exception exceptionToThrow = createExceptionInstance(exceptionName, "Mocked " + exceptionName + " for " + apiCall);

                        if (exceptionToThrow == null) {
                             System.err.println("[WARN] Could not create instance for exception: " + exceptionName);
                             continue; // Skip if exception couldn't be created
                        }

                        // Configure the specific mock method to throw the exception
                        if ("MarketDataService.getRealtimePrice".equals(apiCall)) {
                             // Match the call based on its index (1st or 3rd)
                            if (i == 0 || i == 2) { // Corresponds to buy and sell calls
                                // Use thenAnswer to bypass checked exception validation
                                when(mockMarketData.getRealtimePrice(anyString())).thenAnswer(invocation -> {
                                    throw exceptionToThrow;
                                });
                            }
                        } else if ("StockTradingRepository.executeTradeTransaction".equals(apiCall)) {
                             // Match the call based on its index (2nd or 4th)
                             if (i == 1 || i == 3) { // Corresponds to buy and sell transactions
                                // Use doAnswer to bypass checked exception validation for void methods
                                doAnswer(invocation -> {
                                    throw exceptionToThrow;
                                }).when(mockRepo).executeTradeTransaction(anyString(), anyInt(), anyDouble(), anyString());
                             }
                        }
                        // Add configurations for other mocked methods if the apiCalls list changes
                    }
                }

                // Need to ensure mocks provide necessary state for successful sell if buy fails
                // If buy transaction is mocked to fail, position won't increase.
                // If sell transaction is *not* mocked, it needs a position to sell.
                // Add logic here if necessary, e.g., conditionally set getPosition mock result.
                 if (pattern.get(1).equalsIgnoreCase("normal")) { // If buy transaction is expected to succeed
                     when(mockRepo.getPosition(anyString())).thenReturn(10); // Assume buy succeeded, position is 10 before sell
                 } else {
                     when(mockRepo.getPosition(anyString())).thenReturn(0); // Buy failed, position is 0 before sell
                 }


                // --- Execute Test Logic with Mocks ---
                edu.unl.stock.StockTradingService service = new edu.unl.stock.StockTradingService(mockRepo, mockMarketData);
                StockTradingResource resource = new StockTradingResource(service); // Use the mocked service

                try {
                    resource.buyStock("AAPL", 10);
                    // Only proceed if buyStock didn't throw
                    double balance = resource.getBalance(); // This might now use mocked repo
                    resource.sellStock("AAPL", 5);
                    int position = resource.getPosition("AAPL"); // This might now use mocked repo

                    stat.put("result", "success");
                    stat.put("balance", balance);
                    stat.put("position", position);
                    isSuccess = true;
                    System.out.println("[Test] Succeeded. Pattern: " + pattern);

                } catch (Exception e) { // Catch all exceptions thrown by the mocked flow
                    stat.put("result", "exception");
                    String exceptionSimpleName = e.getClass().getSimpleName();
                    stat.put("exceptionType", exceptionSimpleName);
                    stat.put("exceptionMsg", e.getMessage());
                    ExceptionDetails capturedDetails = buildExceptionDetailsChain(e, pattern.toString());
                    coverageStats.addSutExceptionChain("testAmplifiedBuyAndSell", pattern.toString(), capturedDetails);
                    System.out.println("[Test] Caught Exception: " + exceptionSimpleName + ". Pattern: " + pattern + ". Msg: " + e.getMessage());
                    coveredExceptions.add(exceptionSimpleName); // Update static set
                    coverageStats.addExceptionStat("testAmplifiedBuyAndSell", exceptionSimpleName, true); // Update local reporter
                    // Optionally, check if the caught exception matches the mocked one(s)
                }

            } catch (Exception setupException) {
                 // Catch exceptions during mock setup itself
                 stat.put("result", "error");
                 stat.put("exceptionType", setupException.getClass().getSimpleName());
                 stat.put("exceptionMsg", "Error during mock setup: " + setupException.getMessage());
                 System.err.println("[Test] Error during mock setup for pattern " + pattern + ": " + setupException.getMessage());
                 setupException.printStackTrace(); // Print stack trace for setup errors
            } finally {
                long endTime = System.currentTimeMillis();
                stat.put("isSuccess", isSuccess); // isSuccess reflects if the *entire* sequence completed without *uncaught* exception
                stat.put("timeCostMs", endTime - startTime);
                stat.put("timestamp", endTime);
                // Report path coverage based on whether the test completed without setup error or uncaught runtime error
                 coverageStats.addStat("testAmplifiedBuyAndSell", pattern.toString(), stat.get("result") != "error" && isSuccess);
                 System.out.println("[TestExplorer] Finished pattern: " + pattern + " -> " + (isSuccess ? "Success" : "Exception/Error"));
            }
        });

        System.out.println("[TestExplorer] Exploration finished.");

        // Print reports from the local path coverage reporter
        coverageStats.printSummaryReport();
        coverageStats.printDetailReport();
        coverageStats.printCoverageTree();

        // The @AfterClass method will print the final exception coverage based on the static sets
    }

    private static ExceptionDetails buildExceptionDetailsChain(Throwable throwable, String inputPattern) {
        if (throwable == null) {
            return null;
        }
        List<String> stackTraceList = Arrays.stream(throwable.getStackTrace())
                .map(ste -> String.format("%s.%s(%s:%d)",
                        ste.getClassName(),
                        ste.getMethodName(),
                        ste.getFileName(),
                        ste.getLineNumber()))
                .collect(Collectors.toList());
        ExceptionDetails causeDetails = buildExceptionDetailsChain(throwable.getCause(), inputPattern);
        return new ExceptionDetails(
                throwable.getClass().getName(),
                throwable.getMessage(),
                stackTraceList,
                causeDetails,
                inputPattern
        );
    }

    // Helper method to create exception instances from class names
    private static Exception createExceptionInstance(String className, String message) {
        try {
            // Handle nested class names if necessary, assume top-level for now
            String fullClassName;
            if (className.equals("IOException")) {
                fullClassName = "java.io.IOException";
            } else if (className.equals("SQLException")) {
                fullClassName = "java.sql.SQLException";
            } else if (className.equals("IllegalArgumentException")) {
                 fullClassName = "java.lang.IllegalArgumentException";
            } else {
                // Assume it's in the edu.unl.stock package
                fullClassName = "edu.unl.stock." + className;
            }

            Class<?> clazz = Class.forName(fullClassName);
            // Try to find a constructor that accepts a String message
            try {
                Constructor<?> constructor = clazz.getConstructor(String.class);
                return (Exception) constructor.newInstance(message);
            } catch (NoSuchMethodException e) {
                // Try default constructor if no String constructor exists
                Constructor<?> constructor = clazz.getConstructor();
                return (Exception) constructor.newInstance();
            }
        } catch (Exception e) {
            System.err.println("Failed to instantiate exception: " + className + " - " + e.getMessage());
            // Optionally return a generic RuntimeException or null
             return new RuntimeException("Failed to mock " + className + ": " + message, e);
           // return null;
        }
    }
}
