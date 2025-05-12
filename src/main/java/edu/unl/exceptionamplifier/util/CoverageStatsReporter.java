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
