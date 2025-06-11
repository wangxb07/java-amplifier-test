package edu.unl.exceptionamplifier.testcases;

import edu.unl.exceptionamplifier.builder.ExceptionalSpaceBuilder;
import edu.unl.exceptionamplifier.explorer.TestExplorer;
import edu.unl.exceptionamplifier.util.CoverageStatsReporter;
import edu.unl.exceptionamplifier.util.ExceptionReflectionUtils;
import edu.unl.wallet.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class WalletResourceAmplifiedTest {
    private static final String TEST_TOKEN = "ETH";
    private static final String TEST_TARGET = "0xabc";
    private static final double TEST_AMOUNT = 10.0;
    private static final int K_FOR_EXHAUSTIVE = 2;

    // 假设钱包操作的调用链为：getBalance -> updateBalance -> executeTransfer
    private static final List<String> API_CALL_SEQUENCE = Arrays.asList(
            "walletRepository.getBalance",           // [0] 查询余额
            "walletRepository.updateBalance",        // [1] 更新余额
            "walletRepository.executeTransfer"       // [2] 执行转账
    );

    private static final List<String> ALL_EXCEPTION_TYPES = Arrays.asList(
        "java.io.IOException",
        "java.sql.SQLException",
        "java.util.concurrent.TimeoutException",
        "edu.unl.wallet.InsufficientBalanceException",
        "java.lang.IllegalArgumentException",
        "java.lang.NullPointerException"
    );

    private static CoverageStatsReporter exhaustiveStatsReporter;
    private static CoverageStatsReporter highRiskStatsReporter;
    private static CoverageStatsReporter llmStatsReporter;
    private static CoverageStatsReporter allOpsStatsReporter;
    private static Set<String> overallCoveredExceptions = new HashSet<>();
    private static Map<String, String> serviceClassMapForStats;
    private static int totalPotentialExceptions;
    private ExceptionalSpaceBuilder exceptionSpaceBuilder;

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
        serviceClassMapForStats.put("walletRepository", "edu.unl.wallet.WalletRepository");
        exhaustiveStatsReporter = new CoverageStatsReporter();
        highRiskStatsReporter = new CoverageStatsReporter();
        llmStatsReporter = new CoverageStatsReporter();
        allOpsStatsReporter = new CoverageStatsReporter();
        overallCoveredExceptions.clear();
        exceptionSpaceBuilder = new ExceptionalSpaceBuilder();
        totalPotentialExceptions = ExceptionReflectionUtils.countDeclaredExceptions(API_CALL_SEQUENCE, serviceClassMapForStats);
    }

    @Test
    public void testExhaustiveAmplification() throws Exception {
        System.out.println("\n--- Testing Exhaustive Amplification ---");
        exhaustiveStatsReporter = new CoverageStatsReporter();
        if (exceptionSpaceBuilder == null) {
            exceptionSpaceBuilder = new ExceptionalSpaceBuilder();
        }
        List<List<String>> exhaustivePatterns = exceptionSpaceBuilder.generateMockingPatterns(
                API_CALL_SEQUENCE,
                ALL_EXCEPTION_TYPES,
                ExceptionalSpaceBuilder.PatternGenerationStrategy.EXHAUSTIVE,
                K_FOR_EXHAUSTIVE);
        executeStrategyPatterns("Exhaustive", exhaustivePatterns, exhaustiveStatsReporter);
        exhaustiveStatsReporter.printDetailReport();
    }

    @Test
    public void testHighRiskOnlyAmplification() throws Exception {
        System.out.println("\n--- Running High-Risk Only Amplification ---");
        highRiskStatsReporter = new CoverageStatsReporter();
        exceptionSpaceBuilder.setApiRiskScore("walletRepository.updateBalance", 1.5);
        exceptionSpaceBuilder.setApiRiskScore("walletRepository.crossChainSwap", 1.5);
        List<List<String>> highRiskPatterns = exceptionSpaceBuilder.generateMockingPatterns(
                API_CALL_SEQUENCE,
                ALL_EXCEPTION_TYPES,
                ExceptionalSpaceBuilder.PatternGenerationStrategy.HIGH_RISK_SELECTIVE,
                0);
        executeStrategyPatterns("HighRisk", highRiskPatterns, highRiskStatsReporter);
        highRiskStatsReporter.printDetailReport();
    }

    @Test
    public void testLLMAmplification() throws Exception {
        System.out.println("\n--- Running LLM Amplification ---");
        llmStatsReporter = new CoverageStatsReporter();
        List<List<String>> llmPatterns = exceptionSpaceBuilder.generateMockingPatterns(
                API_CALL_SEQUENCE,
                ALL_EXCEPTION_TYPES,
                ExceptionalSpaceBuilder.PatternGenerationStrategy.LLM_BASED,
                0);
        executeStrategyPatterns("LLM", llmPatterns, llmStatsReporter);
        llmStatsReporter.printDetailReport();
    }

    private void executeStrategyPatterns(String testName, List<List<String>> patterns, CoverageStatsReporter currentPatternReporter) throws Exception {
        for (List<String> currentPattern : patterns) {
            String patternString = String.join(",", currentPattern);
            System.out.println("[Pattern] " + patternString);
            WalletRepository mockRepository = mock(WalletRepository.class);
            WalletService mockService = new WalletService(mockRepository);
            WalletResource sut = new WalletResource(mockService);
            AtomicInteger getBalanceCallCount = new AtomicInteger(0);
            AtomicInteger updateBalanceCallCount = new AtomicInteger(0);
            AtomicInteger crossChainSwapCallCount = new AtomicInteger(0);
            // mock getBalance
            when(mockRepository.getBalance(anyString())).thenAnswer(inv -> {
                getBalanceCallCount.incrementAndGet();
                if (!"normal".equals(currentPattern.get(0))) {
                    throw ExceptionReflectionUtils.createExceptionInstance(currentPattern.get(0), "Mocked for getBalance");
                }
                return 100.0;
            });
            // mock updateBalance
            doAnswer(inv -> {
                updateBalanceCallCount.incrementAndGet();
                if (!"normal".equals(currentPattern.get(1))) {
                    throw ExceptionReflectionUtils.createExceptionInstance(currentPattern.get(1), "Mocked for updateBalance");
                }
                return null;
            }).when(mockRepository).updateBalance(anyString(), anyDouble());
            // mock crossChainSwap
            doAnswer(inv -> {
                crossChainSwapCallCount.incrementAndGet();
                if (!"normal".equals(currentPattern.get(2))) {
                    throw ExceptionReflectionUtils.createExceptionInstance(currentPattern.get(2), "Mocked for crossChainSwap");
                }
                return null;
            }).when(mockRepository).crossChainSwap(anyString(), anyString(), anyDouble());

            try {
                sut.deposit(TEST_TOKEN, TEST_AMOUNT);
                sut.transfer(TEST_TOKEN, TEST_TARGET, TEST_AMOUNT);
                sut.crossChainSwap(TEST_TOKEN, "BNB", TEST_AMOUNT);
                currentPatternReporter.addStat(testName, patternString + " -> All Ops OK", true);
            } catch (Exception e) {
                String exceptionType = e.getClass().getName();
                System.out.println("  [SUT Exception] Caught: " + exceptionType + " for pattern: " + patternString + " Message: " + e.getMessage());
                currentPatternReporter.addExceptionStat(testName, exceptionType);
                overallCoveredExceptions.add(exceptionType);
                CoverageStatsReporter.ExceptionDetails details = buildSutExceptionDetailsChain(e, patternString);
                currentPatternReporter.addSutExceptionChain(testName, patternString, details);
            }
        }
    }
}
