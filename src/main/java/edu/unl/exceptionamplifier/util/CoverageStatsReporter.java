package edu.unl.exceptionamplifier.util;

import java.util.*;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;

public class CoverageStatsReporter {
    private final Map<String, List<String>> normalPathStats = new HashMap<>();
    private final Map<String, Set<String>> exceptionStats = new HashMap<>();
    private final Map<String, Set<String>> requiredExceptions = new HashMap<>();
    private final Map<String, Set<String>> coveredExceptions = new HashMap<>();
    private final Map<String, List<ExceptionDetails>> detailedExceptionStats = new HashMap<>();

    public static class ExceptionDetails {
        String exceptionType;
        String message;
        List<String> stackTrace;
        ExceptionDetails cause;
        String injectedByPattern;

        public ExceptionDetails(String exceptionType, String message, List<String> stackTrace, ExceptionDetails cause, String injectedByPattern) {
            this.exceptionType = exceptionType;
            this.message = message;
            this.stackTrace = stackTrace;
            this.cause = cause;
            this.injectedByPattern = injectedByPattern;
        }
    }

    public CoverageStatsReporter() {
        // 初始化需要覆盖的异常类型
        initializeRequiredExceptions();
    }

    private void initializeRequiredExceptions() {
        // 业务异常
        Set<String> businessExceptions = new HashSet<>(Arrays.asList(
            "InsufficientBalanceException",
            "PositionNotEnoughException",
            "RemoteApiException"
        ));
        requiredExceptions.put("Business", businessExceptions);

        // 系统异常
        Set<String> systemExceptions = new HashSet<>(Arrays.asList(
            "SQLException",
            "TimeoutException",
            "IOException"
        ));
        requiredExceptions.put("System", systemExceptions);

        // 参数校验异常
        Set<String> validationExceptions = new HashSet<>(Arrays.asList(
            "IllegalArgumentException"
        ));
        requiredExceptions.put("Validation", validationExceptions);

        // 初始化已覆盖的异常集合
        coveredExceptions.put("Business", new HashSet<>());
        coveredExceptions.put("System", new HashSet<>());
        coveredExceptions.put("Validation", new HashSet<>());
    }

    public void addStat(String testName, String pattern, boolean covered) {
        normalPathStats.computeIfAbsent(testName, k -> new ArrayList<>()).add(pattern);
    }

    public void addExceptionStat(String testName, String exceptionType, boolean handled) {
        exceptionStats.computeIfAbsent(testName, k -> new HashSet<>()).add(exceptionType);
        
        // 根据异常类型分类记录
        if (requiredExceptions.get("Business").contains(exceptionType)) {
            coveredExceptions.get("Business").add(exceptionType);
        } else if (requiredExceptions.get("System").contains(exceptionType)) {
            coveredExceptions.get("System").add(exceptionType);
        } else if (requiredExceptions.get("Validation").contains(exceptionType)) {
            coveredExceptions.get("Validation").add(exceptionType);
        }
    }

    public void addSutExceptionChain(String testName, String patternString, ExceptionDetails exceptionChainDetails) {
        String key = testName + "::" + patternString;
        detailedExceptionStats.computeIfAbsent(key, k -> new ArrayList<>()).add(exceptionChainDetails);
    }

    public void printSummaryReport() {
        // 打印常规路径覆盖率
        int totalPaths = 0;
        int coveredPaths = 0;
        for (List<String> patterns : normalPathStats.values()) {
            totalPaths += patterns.size();
            coveredPaths += patterns.stream().filter(p -> p.contains("normal")).count();
        }
        double normalCoverage = totalPaths == 0 ? 0 : (coveredPaths * 100.0 / totalPaths);
        System.out.printf("\n[常规覆盖率统计] 共 %d 条, 覆盖 %d 条, 覆盖率 %.2f%%\n", 
            totalPaths, coveredPaths, normalCoverage);

        // 打印异常覆盖率
        System.out.println("\n[异常覆盖率统计]");
        for (String category : requiredExceptions.keySet()) {
            Set<String> required = requiredExceptions.get(category);
            Set<String> covered = coveredExceptions.get(category);
            int total = required.size();
            int coveredCount = 0;
            for (String ex : required) {
                if (covered.contains(ex)) coveredCount++;
            }
            double rate = total == 0 ? 100.0 : (coveredCount * 100.0 / total);
            System.out.printf("%s异常: %d/%d = %.2f%%\n", 
                category, coveredCount, total, rate);
        }

        // 打印Cobertura报告位置
        System.out.println("\n[Cobertura 覆盖率报告]");
        System.out.println("HTML 报告位置: target/site/cobertura/index.html");
        System.out.println("XML 报告位置: target/site/cobertura/coverage.xml");

        // 打印SUT异常分析报告
        printSutExceptionAnalysisReport();
    }

    private List<String> filterStackTrace(List<String> fullStackTrace, String packagePrefix) {
        if (fullStackTrace == null) {
            return Collections.emptyList();
        }
        return fullStackTrace.stream()
                             .filter(line -> line.startsWith(packagePrefix))
                             .collect(Collectors.toList());
    }

    public void printSutExceptionAnalysisReport() {
        System.out.println("\n[SUT 异常详细分析报告]");
        if (detailedExceptionStats.isEmpty()) {
            System.out.println("没有捕获到SUT异常详细信息。");
            return;
        }

        for (Map.Entry<String, List<ExceptionDetails>> entry : detailedExceptionStats.entrySet()) {
            System.out.println("\nTest Pattern Key: " + entry.getKey());
            for (ExceptionDetails details : entry.getValue()) {
                System.out.println("  Injected Exception Pattern: " + details.injectedByPattern);
                printExceptionChainRecursive(details, "  ");
            }
        }
    }

    private void printExceptionChainRecursive(ExceptionDetails details, String indent) {
        if (details == null) {
            return;
        }
        System.out.println(indent + "Exception: " + details.exceptionType);
        System.out.println(indent + "  Message: " + details.message);

        // Filter and print stack trace for "edu.unl.stock"
        List<String> filteredStackTrace = filterStackTrace(details.stackTrace, "edu.unl.stock");
        if (!filteredStackTrace.isEmpty()) {
            System.out.println(indent + "  SUT Stack Trace (edu.unl.stock):");
            for (String traceLine : filteredStackTrace) {
                System.out.println(indent + "    " + traceLine);
            }
        } else if (details.stackTrace != null && !details.stackTrace.isEmpty()) {
            // Optional: print a few lines of the full stack trace if no specific package matches
            System.out.println(indent + "  SUT Stack Trace (Top 3 lines):");
            details.stackTrace.stream().limit(3).forEach(line -> System.out.println(indent + "    " + line));
        }


        if (details.cause != null) {
            System.out.println(indent + "  Caused by:");
            printExceptionChainRecursive(details.cause, indent + "    ");
        }
    }

    public void printDetailReport() {
        System.out.println("\n[详细覆盖率结果]\n");
        
        // 打印常规路径覆盖详情
        System.out.println("常规路径覆盖:");
        for (Map.Entry<String, List<String>> entry : normalPathStats.entrySet()) {
            String testName = entry.getKey();
            List<String> patterns = entry.getValue();
            for (String pattern : patterns) {
                System.out.printf("%s %s\t\t%s\n", 
                    testName, pattern, pattern.contains("normal") ? "✔" : "✘");
            }
        }

        // 打印异常处理覆盖详情
        System.out.println("\n异常处理覆盖:");
        for (Map.Entry<String, Set<String>> entry : exceptionStats.entrySet()) {
            String testName = entry.getKey();
            Set<String> exceptions = entry.getValue();
            for (String ex : exceptions) {
                System.out.printf("%s: %s\t\t%s\n", 
                    testName, ex, "✔");
            }
        }
    }

    public void printCoverageTree() {
        System.out.println("\n[覆盖率树结构]\n");
        
        // 打印常规路径覆盖树
        System.out.println("常规路径覆盖树:");
        System.out.println("ROOT");
        for (Map.Entry<String, List<String>> entry : normalPathStats.entrySet()) {
            List<String> patterns = entry.getValue();
            for (String pattern : patterns) {
                System.out.printf("  %s => [%s]\n", 
                    pattern, pattern.contains("normal") ? "✔" : "✘");
            }
        }

        // 打印异常处理覆盖树
        System.out.println("\n异常处理覆盖树:");
        System.out.println("EXCEPTIONS");
        for (String category : requiredExceptions.keySet()) {
            System.out.println(category + ":");
            Set<String> required = requiredExceptions.get(category);
            Set<String> covered = coveredExceptions.get(category);
            for (String ex : required) {
                System.out.printf("  %s => [%s]\n", 
                    ex, covered.contains(ex) ? "✔" : "✘");
            }
        }
    }
}
