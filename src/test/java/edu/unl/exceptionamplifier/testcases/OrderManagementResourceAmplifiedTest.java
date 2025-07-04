package edu.unl.exceptionamplifier.testcases;

import edu.unl.exceptionamplifier.builder.ExceptionalSpaceBuilder;
import edu.unl.exceptionamplifier.explorer.TestExplorer;
import edu.unl.exceptionamplifier.util.CoverageStatsReporter;
import edu.unl.exceptionamplifier.util.ExceptionReflectionUtils;
import edu.unl.order.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.*;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrderManagementResourceAmplifiedTest {
    private static final String TEST_PRODUCT = "BOOK";
    private static final int TEST_QUANTITY = 2;
    private static final int K_FOR_EXHAUSTIVE = 2;

    // API call sequence for placeOrder and cancelOrder
    private static final List<String> API_CALL_SEQUENCE = Arrays.asList(
            "orderRepository.getInventory",      // [0] For inventory check
            "orderRepository.getBalance",        // [1] For balance check
            "orderRepository.executeOrder",      // [2] For order execution
            "orderRepository.getOrder",          // [3] For order retrieval (cancel)
            "orderRepository.cancelOrder"        // [4] For order cancellation
    );

    private static final List<String> ALL_EXCEPTION_TYPES = Arrays.asList(
            "java.io.IOException",
            "java.sql.SQLException",
            "java.util.concurrent.TimeoutException",
            "edu.unl.order.InsufficientBalanceException",
            "edu.unl.order.InventoryNotEnoughException",
            "edu.unl.order.RemoteApiException",
            "java.lang.IllegalArgumentException",
            "java.lang.NullPointerException"
    );

    private static CoverageStatsReporter exhaustiveStatsReporter;
    private static Set<String> overallCoveredExceptions = new HashSet<>();
    private static Map<String, String> serviceClassMapForStats;
    private ExceptionalSpaceBuilder exceptionSpaceBuilder;

    private static CoverageStatsReporter.ExceptionDetails buildSutExceptionDetailsChain(Throwable throwable, String injectedByPattern) {
        if (throwable == null) return null;
        List<String> stackTraceList = new ArrayList<>();
        for (StackTraceElement ste : throwable.getStackTrace()) {
            stackTraceList.add(ste.toString());
        }
        CoverageStatsReporter.ExceptionDetails causeDetails = buildSutExceptionDetailsChain(throwable.getCause(), injectedByPattern);
        return new CoverageStatsReporter.ExceptionDetails(
                throwable.getClass().getName(),
                throwable.getMessage(),
                stackTraceList,
                causeDetails,
                injectedByPattern
        );
    }

    @BeforeEach
    public void setUp() throws Exception {
        serviceClassMapForStats = new HashMap<>();
        serviceClassMapForStats.put("orderRepository", "edu.unl.order.OrderRepository");
    }

    @Test
    public void testExhaustiveAmplification() throws Exception {
        exceptionSpaceBuilder = new ExceptionalSpaceBuilder();
        List<List<String>> patterns = exceptionSpaceBuilder.generateMockingPatterns(
                API_CALL_SEQUENCE,
                ALL_EXCEPTION_TYPES,
                ExceptionalSpaceBuilder.PatternGenerationStrategy.EXHAUSTIVE,
                K_FOR_EXHAUSTIVE);
        exhaustiveStatsReporter = new CoverageStatsReporter();
        executeStrategyPatterns("Exhaustive", patterns, exhaustiveStatsReporter);
        exhaustiveStatsReporter.printSummaryReport();
    }

    @Test
    public void testLlmAmplification() throws Exception {
        exceptionSpaceBuilder = new ExceptionalSpaceBuilder();
        List<List<String>> patterns = exceptionSpaceBuilder.generateMockingPatterns(
            API_CALL_SEQUENCE, ALL_EXCEPTION_TYPES, 
                ExceptionalSpaceBuilder.PatternGenerationStrategy.LLM_BASED, 0);
        CoverageStatsReporter llmStatsReporter = new CoverageStatsReporter();
        executeStrategyPatterns("LLM", patterns, llmStatsReporter);
        llmStatsReporter.printSummaryReport();
    }

    private void executeStrategyPatterns(String testName, List<List<String>> patterns, CoverageStatsReporter currentPatternReporter) {
        patterns.forEach(currentPattern -> {
            String patternString = String.join(",", currentPattern);
            System.out.println("[Pattern] " + patternString);
            OrderRepository mockRepository = mock(OrderRepository.class);
            ProductPriceService mockPriceService = mock(ProductPriceService.class);
            OrderManagementService service = new OrderManagementService(mockRepository, mockPriceService);
            OrderManagementResource resource = new OrderManagementResource(service);

            // Call counters for sequence
            final int[] inventoryCallCount = {0};
            final int[] balanceCallCount = {0};
            final int[] placeOrderCallCount = {0};
            final int[] cancelOrderCallCount = {0};
            final int[] priceCallCount = {0};

            try {
                // Mock getInventory
                when(mockRepository.getInventory(anyString())).thenAnswer(inv -> {
                    inventoryCallCount[0]++;
                    if (!"normal".equals(currentPattern.get(0))) {
                        throw ExceptionReflectionUtils.createExceptionInstance(currentPattern.get(0), "Mocked for getInventory");
                    }
                    return 5;
                });

                // Mock getBalance
                when(mockRepository.getBalance()).thenAnswer(inv -> {
                    balanceCallCount[0]++;
                    if (!"normal".equals(currentPattern.get(1))) {
                        throw ExceptionReflectionUtils.createExceptionInstance(currentPattern.get(1), "Mocked for getBalance");
                    }
                    return 500.0;
                });

                // Mock placeOrder (simulate order placement, return orderId)
                doAnswer(inv -> {
                    placeOrderCallCount[0]++;
                    if (!"normal".equals(currentPattern.get(2))) {
                        throw ExceptionReflectionUtils.createExceptionInstance(currentPattern.get(2), "Mocked for placeOrder");
                    }
                    return 123; // mock orderId
                }).when(mockRepository).placeOrder(anyString(), anyInt(), anyDouble());


                // Mock cancelOrder (simulate order cancellation)
                doAnswer(inv -> {
                    cancelOrderCallCount[0]++;
                    if (!"normal".equals(currentPattern.get(4))) {
                        throw ExceptionReflectionUtils.createExceptionInstance(currentPattern.get(4), "Mocked for cancelOrder");
                    }
                    return null;
                }).when(mockRepository).cancelOrder(anyInt());

                // Mock price service
                when(mockPriceService.getPrice(anyString())).thenAnswer(inv -> {
                    priceCallCount[0]++;
                    return 50.0;
                });

                // Place and cancel order sequence
                System.out.println("  [SUT] Attempting to place order");
                int orderId = resource.placeOrder(TEST_PRODUCT, TEST_QUANTITY);
                currentPatternReporter.addStat(testName, patternString + " -> placeOrder OK", true);
                System.out.println("  [SUT] Place order successful.");

                System.out.println("  [SUT] Attempting to cancel order");
                resource.cancelOrder(orderId);
                currentPatternReporter.addStat(testName, patternString + " -> cancelOrder OK", true);
                System.out.println("  [SUT] Cancel order successful.");

                currentPatternReporter.addStat(testName, patternString + " -> Full sequence OK", true);

            } catch (Exception e) {
                String exceptionType = e.getClass().getName();
                System.out.println("  [SUT Exception] Caught: " + exceptionType + " for pattern: " + patternString + " Message: " + e.getMessage());
                currentPatternReporter.addExceptionStat(testName, exceptionType);
                overallCoveredExceptions.add(exceptionType);
                CoverageStatsReporter.ExceptionDetails details = buildSutExceptionDetailsChain(e, patternString);
                currentPatternReporter.addSutExceptionChain(testName, patternString, details);
            } finally {
                int expectedInventoryCalls = 1;
                int expectedBalanceCalls = "normal".equals(currentPattern.get(0)) ? 1 : 0;
                int expectedPlaceOrderCalls = (expectedBalanceCalls == 1 && "normal".equals(currentPattern.get(1))) ? 1 : 0;
                int expectedCancelOrderCalls = (expectedPlaceOrderCalls == 1 && "normal".equals(currentPattern.get(2))) ? 1 : 0;
                assertEquals(expectedInventoryCalls, inventoryCallCount[0], "inventory calls");
                assertEquals(expectedBalanceCalls, balanceCallCount[0], "balance calls");
                assertEquals(expectedPlaceOrderCalls, placeOrderCallCount[0], "placeOrder calls");
                assertEquals(expectedCancelOrderCalls, cancelOrderCallCount[0], "cancelOrder calls");
                assertEquals(1, priceCallCount[0], "price calls");
            }
        });
    }
}
