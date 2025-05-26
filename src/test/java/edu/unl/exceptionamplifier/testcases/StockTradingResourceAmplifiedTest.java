package edu.unl.exceptionamplifier.testcases;

import edu.unl.exceptionamplifier.builder.ExceptionalSpaceBuilder;
import edu.unl.exceptionamplifier.explorer.TestExplorer;
import edu.unl.exceptionamplifier.util.CoverageStatsReporter;
import edu.unl.exceptionamplifier.util.ExceptionReflectionUtils;
import edu.unl.stock.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StockTradingResourceAmplifiedTest {

    private static final String TEST_STOCK_SYMBOL = "AAPL";
    private static final int TEST_QUANTITY = 10;
    private static final int K_FOR_EXHAUSTIVE = 2;

    private static final List<String> API_CALL_SEQUENCE = Arrays.asList(
            // Buy Operation
            "marketDataService.getRealtimePrice",           // [0] For buy price check
            "stockTradingRepository.getPosition",           // [1] For buy, initial position check (even if not strictly used for decision)
            "stockTradingRepository.getBalance",            // [2] For buy, balance check
            "stockTradingRepository.executeTradeTransaction", // [3] For buy execution
            // Sell Operation
            "stockTradingRepository.getPosition",           // [4] For sell, position check
            "marketDataService.getRealtimePrice",           // [5] For sell price check
            "stockTradingRepository.executeTradeTransaction"  // [6] For sell execution
    );

    private static final List<String> ALL_EXCEPTION_TYPES = Arrays.asList(
        "java.io.IOException",
        "java.sql.SQLException",
        "java.util.concurrent.TimeoutException",
        "edu.unl.stock.InsufficientBalanceException",
        "edu.unl.stock.PositionNotEnoughException",
        "edu.unl.stock.RemoteApiException",
        "java.lang.IllegalArgumentException",
        "java.lang.NullPointerException"
    );

    private static CoverageStatsReporter exhaustiveStatsReporter;
    private static CoverageStatsReporter highRiskStatsReporter;
    private static CoverageStatsReporter llmStatsReporter;
    private static CoverageStatsReporter buyAndSellStatsReporter;
    private static Set<String> overallCoveredExceptions = new HashSet<>();
    private static Map<String, String> serviceClassMapForStats;
    private static int totalPotentialExceptions;
    private ExceptionalSpaceBuilder exceptionSpaceBuilder;
    
    // Helper method to build the ExceptionDetails chain for CoverageStatsReporter
    private static CoverageStatsReporter.ExceptionDetails buildSutExceptionDetailsChain(Throwable throwable, String injectedByPattern) {
        if (throwable == null) {
            return null;
        }
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
        serviceClassMapForStats.put("marketDataService", "edu.unl.stock.MarketDataService");
        serviceClassMapForStats.put("stockTradingRepository", "edu.unl.stock.StockTradingRepository");

        totalPotentialExceptions = ExceptionReflectionUtils.countDeclaredExceptions(API_CALL_SEQUENCE, serviceClassMapForStats);
        if (totalPotentialExceptions == 0) {
            System.err.println("Warning: totalPotentialExceptions is 0. Check API_CALL_SEQUENCE and FQCNs in serviceClassMapForStats.");
        }

        exceptionSpaceBuilder = new ExceptionalSpaceBuilder();
        buyAndSellStatsReporter = new CoverageStatsReporter();
        exhaustiveStatsReporter = new CoverageStatsReporter();
        highRiskStatsReporter = new CoverageStatsReporter();
        llmStatsReporter = new CoverageStatsReporter();
        overallCoveredExceptions.clear();
    }

    @Test
    public void testExhaustiveAmplification() throws Exception {
        System.out.println("\n--- Testing Exhaustive Amplification ---");
        exhaustiveStatsReporter = new CoverageStatsReporter(); 

        if (exceptionSpaceBuilder == null) {
            System.err.println("[ERROR] exceptionSpaceBuilder is null in testExhaustiveAmplification!");
            exceptionSpaceBuilder = new ExceptionalSpaceBuilder(); 
        }

        List<List<String>> exhaustivePatterns = exceptionSpaceBuilder.generateMockingPatterns(
                API_CALL_SEQUENCE,
                ALL_EXCEPTION_TYPES,
                ExceptionalSpaceBuilder.PatternGenerationStrategy.EXHAUSTIVE, 
                K_FOR_EXHAUSTIVE);

        System.out.println("[DEBUG] Exhaustive patterns generated: " + (exhaustivePatterns == null ? "null" : exhaustivePatterns.size()));
        if (exhaustivePatterns != null && !exhaustivePatterns.isEmpty()) {
            System.out.println("[DEBUG] First exhaustive pattern: " + exhaustivePatterns.get(0));
        } else if (exhaustivePatterns != null && exhaustivePatterns.isEmpty()) {
            System.out.println("[DEBUG] Exhaustive patterns list is empty.");
        }

        List<String> allNormalPattern = Collections.nCopies(API_CALL_SEQUENCE.size(), "normal");
        boolean foundAllNormal = false;
        if (exhaustivePatterns != null) {
            for (List<String> p : exhaustivePatterns) {
                if (p.equals(allNormalPattern)) {
                    foundAllNormal = true;
                    break;
                }
            }
        }
        System.out.println("[DEBUG] Is 'all normal' pattern present in generated patterns? " + foundAllNormal);
        if (foundAllNormal) {
            System.out.println("[DEBUG] 'All normal' pattern is: " + allNormalPattern);
        }

        if (foundAllNormal && exhaustivePatterns != null && !exhaustivePatterns.isEmpty()) {
            exhaustivePatterns.remove(allNormalPattern); // 先移除
            exhaustivePatterns.add(0, allNormalPattern); // 再添加到开头
            System.out.println("[DEBUG] Moved 'all normal' pattern to the beginning of the execution list.");
            System.out.println("[DEBUG] First pattern to execute NOW: " + exhaustivePatterns.get(0));
        }

        // Ensure the reporter is fresh for this strategy
        executeStrategyPatterns("Exhaustive", exhaustivePatterns, exhaustiveStatsReporter);
        exhaustiveStatsReporter.printDetailReport(); 
    }

    @Test
    public void testHighRiskOnlyAmplification() throws Exception {
        System.out.println("\n--- Running High-Risk Only Amplification ---");
        highRiskStatsReporter = new CoverageStatsReporter(); // Ensure reporter is fresh

        exceptionSpaceBuilder.setApiRiskScore("marketDataService.getRealtimePrice", 1.0); 
        exceptionSpaceBuilder.setApiRiskScore("stockTradingRepository.executeTradeTransaction", 1.5); 

        List<List<String>> highRiskPatterns = exceptionSpaceBuilder.generateMockingPatterns(
                API_CALL_SEQUENCE,
                ALL_EXCEPTION_TYPES,
                ExceptionalSpaceBuilder.PatternGenerationStrategy.HIGH_RISK_SELECTIVE,
                0); 
        System.out.println("Generated " + highRiskPatterns.size() + " high-risk patterns.");
        executeStrategyPatterns("HighRisk", highRiskPatterns, highRiskStatsReporter);
        highRiskStatsReporter.printDetailReport();
    }

    @Test
    public void testLLMAmplification() throws Exception {
        System.out.println("\n--- Running LLM Amplification ---");
        llmStatsReporter = new CoverageStatsReporter(); // Ensure reporter is fresh

        List<List<String>> llmPatterns = exceptionSpaceBuilder.generateMockingPatterns(
                API_CALL_SEQUENCE,
                ALL_EXCEPTION_TYPES,
                ExceptionalSpaceBuilder.PatternGenerationStrategy.LLM_BASED,
                0); 
        System.out.println("Generated " + llmPatterns.size() + " LLM patterns.");
        if (llmPatterns.isEmpty()) {
            System.out.println("WARNING: LLM generated no patterns. Check API key, network, LLM service, or prompt in ExceptionalSpaceBuilder.");
        }
        executeStrategyPatterns("LLM", llmPatterns, llmStatsReporter);
        llmStatsReporter.printDetailReport();
    }

    @Test
    void testAmplifiedBuyAndSellStrategies() throws Exception {
        System.out.println("Total potential exceptions declared in API sequence: " + totalPotentialExceptions);

        List<List<String>> patterns = exceptionSpaceBuilder.generateMockingPatterns(API_CALL_SEQUENCE, ALL_EXCEPTION_TYPES);
        executeStrategyPatterns("BuyAndSell", patterns, buyAndSellStatsReporter);
    }

    private void executeStrategyPatterns(String testName, List<List<String>> patterns, CoverageStatsReporter currentPatternReporter) throws Exception {
        TestExplorer explorer = new TestExplorer();

        explorer.explore(API_CALL_SEQUENCE, patterns, (List<String> currentPattern) -> {
            String patternString = String.join(", ", currentPattern);
            System.out.println(testName + " - Executing pattern: " + patternString);

            MarketDataService mockMarketService = mock(MarketDataService.class);
            StockTradingRepository mockRepository = mock(StockTradingRepository.class);
            StockTradingService stockTradingService = new StockTradingService(mockRepository, mockMarketService);
            StockTradingResource sut = new StockTradingResource(stockTradingService);

            final AtomicInteger mdsGetPriceCallCount = new AtomicInteger(0);
            final AtomicInteger repoGetPositionCallCount = new AtomicInteger(0);
            final AtomicInteger repoGetBalanceCallCount = new AtomicInteger(0);
            final AtomicInteger repoExecTradeCallCount = new AtomicInteger(0);
            
            when(mockMarketService.getRealtimePrice(anyString())).thenAnswer(inv -> {
                int callNum = mdsGetPriceCallCount.incrementAndGet();
                if (callNum == 1) { 
                    if (!"normal".equals(currentPattern.get(0))) {
                        System.out.println("  [Mock] MDS.getRealtimePrice (buy) throwing " + currentPattern.get(0));
                        throw ExceptionReflectionUtils.createExceptionInstance(currentPattern.get(0), "Mocked for MDS.getRealtimePrice (buy)");
                    }
                    return 100.0; 
                } else if (callNum == 2) { 
                    if (!"normal".equals(currentPattern.get(5))) {
                        System.out.println("  [Mock] MDS.getRealtimePrice (sell) throwing " + currentPattern.get(5));
                        throw ExceptionReflectionUtils.createExceptionInstance(currentPattern.get(5), "Mocked for MDS.getRealtimePrice (sell)");
                    }
                    return 110.0; 
                }
                System.out.println("  [Mock] MDS.getRealtimePrice unexpected call " + callNum + ", returning default 100.0");
                return 100.0;
            });

            when(mockRepository.getPosition(anyString())).thenAnswer(inv -> {
                int callNum = repoGetPositionCallCount.incrementAndGet();
                if (callNum == 1) { 
                    if (!"normal".equals(currentPattern.get(1))) {
                        System.out.println("  [Mock] Repo.getPosition (buy) throwing " + currentPattern.get(1));
                        throw ExceptionReflectionUtils.createExceptionInstance(currentPattern.get(1), "Mocked for Repo.getPosition (buy)");
                    }
                    return 0; 
                } else if (callNum == 2) { 
                    if (!"normal".equals(currentPattern.get(4))) {
                        System.out.println("  [Mock] Repo.getPosition (sell) throwing " + currentPattern.get(4));
                        throw ExceptionReflectionUtils.createExceptionInstance(currentPattern.get(4), "Mocked for Repo.getPosition (sell)");
                    }
                    return TEST_QUANTITY + 10; 
                }
                System.out.println("  [Mock] Repo.getPosition unexpected call " + callNum + ", returning 0");
                return 0;
            });

            when(mockRepository.getBalance()).thenAnswer(inv -> {
                repoGetBalanceCallCount.incrementAndGet(); 
                if (!"normal".equals(currentPattern.get(2))) {
                    System.out.println("  [Mock] Repo.getBalance throwing " + currentPattern.get(2));
                    throw ExceptionReflectionUtils.createExceptionInstance(currentPattern.get(2), "Mocked for Repo.getBalance");
                }
                return 100000.0; 
            });

            doAnswer(inv -> {
                int callNum = repoExecTradeCallCount.incrementAndGet();
                if (callNum == 1) { 
                    if (!"normal".equals(currentPattern.get(3))) {
                        System.out.println("  [Mock] Repo.executeTradeTransaction (buy) throwing " + currentPattern.get(3));
                        throw ExceptionReflectionUtils.createExceptionInstance(currentPattern.get(3), "Mocked for Repo.executeTradeTransaction (buy)");
                    }
                } else if (callNum == 2) { 
                    if (!"normal".equals(currentPattern.get(6))) {
                        System.out.println("  [Mock] Repo.executeTradeTransaction (sell) throwing " + currentPattern.get(6));
                        throw ExceptionReflectionUtils.createExceptionInstance(currentPattern.get(6), "Mocked for Repo.executeTradeTransaction (sell)");
                    }
                }
                if (callNum > 2) {
                     System.out.println("  [Mock] Repo.executeTradeTransaction unexpected call " + callNum);
                }
                return null;
            }).when(mockRepository).executeTradeTransaction(anyString(), anyInt(), anyDouble(), anyString());

            try {
                System.out.println("  [SUT] Attempting to buy " + TEST_QUANTITY + " of " + TEST_STOCK_SYMBOL);
                sut.buyStock(TEST_STOCK_SYMBOL, TEST_QUANTITY);
                currentPatternReporter.addStat(testName, patternString + " -> buyStock OK", true); 
                System.out.println("  [SUT] Buy stock successful.");

                System.out.println("  [SUT] Attempting to sell " + TEST_QUANTITY + " of " + TEST_STOCK_SYMBOL);
                sut.sellStock(TEST_STOCK_SYMBOL, TEST_QUANTITY);
                currentPatternReporter.addStat(testName, patternString + " -> sellStock OK", true); 
                System.out.println("  [SUT] Sell stock successful.");

                currentPatternReporter.addStat(testName, patternString + " -> Full sequence OK", true); 

            } catch (Exception e) { 
                String exceptionType = e.getClass().getName();
                System.out.println("  [SUT Exception] Caught: " + exceptionType + " for pattern: " + patternString + " Message: " + e.getMessage());
                currentPatternReporter.addExceptionStat(testName, exceptionType);
                overallCoveredExceptions.add(exceptionType);
                CoverageStatsReporter.ExceptionDetails details = buildSutExceptionDetailsChain(e, patternString); 
                currentPatternReporter.addSutExceptionChain(testName, patternString, details);
            }
        });
    }
}
