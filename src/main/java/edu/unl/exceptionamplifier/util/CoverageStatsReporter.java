package edu.unl.exceptionamplifier.util;

import java.util.*;
import java.util.stream.Collectors;

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
    }

    public void addStat(String testName, String pattern, boolean covered) {
        normalPathStats.computeIfAbsent(testName, k -> new ArrayList<>()).add(pattern);
    }

    public void addExceptionStat(String testName, String exceptionType) {
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
        int totalPaths = 0;
        int coveredNormalPaths = 0;
        for (List<String> patterns : normalPathStats.values()) {
            totalPaths += patterns.size();
            coveredNormalPaths += patterns.stream().filter(p -> p.contains("normal")).count(); 
        }
        double normalCoverage = totalPaths == 0 ? 0 : (coveredNormalPaths * 100.0 / totalPaths);
        System.out.printf("\n[常规路径覆盖率统计] 共 %d 条路径组合, 预期内执行 %d 条, 覆盖率 %.2f%%\n",
            totalPaths, coveredNormalPaths, normalCoverage);

        System.out.println("\n[捕获异常统计]");
        if (coveredExceptions.isEmpty()) {
            System.out.println("未捕获任何异常。");
        } else {
            for (Map.Entry<String, Integer> entry : coveredExceptions.entrySet()) {
                System.out.printf("%s: %d 次\n", entry.getKey(), entry.getValue());
            }
        }

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

        System.out.println("常规路径覆盖:");
        if (normalPathStats.isEmpty()) {
            System.out.println("  无常规路径执行记录。");
        }
        for (Map.Entry<String, List<String>> entry : normalPathStats.entrySet()) {
            String testName = entry.getKey();
            List<String> patterns = entry.getValue();
            System.out.printf("  测试: %s\n", testName);
            for (String pattern : patterns) {
                System.out.printf("    路径组合: %s [%s]\n",
                    pattern, pattern.contains("normal") ? "预期内" : "其他");
            }
        }

        System.out.println("\n异常捕获详情:");
        if (exceptionStats.isEmpty()) {
            System.out.println("  无异常捕获记录。");
        }
        for (Map.Entry<String, Set<String>> entry : exceptionStats.entrySet()) {
            String testName = entry.getKey();
            Set<String> exceptionsInTest = entry.getValue();
            System.out.printf("  测试: %s 捕获的异常类型:\n", testName);
            for (String ex : exceptionsInTest) {
                System.out.printf("    - %s\n", ex);
            }
        }
    }

    public void printCoverageTree() {
        System.out.println("\n[覆盖情况树结构]\n");

        System.out.println("常规路径:");
        if (normalPathStats.isEmpty()) {
            System.out.println("  无常规路径执行记录。");
        } else {
            System.out.println("  ROOT_NORMAL_PATHS");
            Map<String, Long> patternCounts = normalPathStats.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));
            
            for (Map.Entry<String, Long> entry : patternCounts.entrySet()) {
                 System.out.printf("    %s (%d 次) [%s]\n",
                    entry.getKey(), entry.getValue(), entry.getKey().contains("normal") ? "预期内" : "其他");
            }
        }

        System.out.println("\n捕获的异常类型及总次数:");
        if (coveredExceptions.isEmpty()) {
            System.out.println("  未捕获任何异常。");
        } else {
            System.out.println("  ROOT_EXCEPTIONS");
            for (Map.Entry<String, Integer> entry : coveredExceptions.entrySet()) {
                System.out.printf("    %s (总计: %d 次)\n",
                    entry.getKey(), entry.getValue());
            }
        }
    }
}
