package edu.unl.exceptionamplifier.testcases;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用于收集、管理和报告放大测试覆盖统计的工具类。
 * 支持树状结构展示pattern覆盖情况。
 */
public class CoverageStatsReporter {
    private final List<Map<String, Object>> stats = new ArrayList<>();

    public void addStat(Map<String, Object> stat) {
        stats.add(stat);
    }

    public void addAll(Collection<Map<String, Object>> statsList) {
        stats.addAll(statsList);
    }

    /**
     * 打印简要统计报告。
     */
    public void printSummaryReport() {
        long successCount = stats.stream().filter(s -> "success".equals(s.get("result"))).count();
        long exceptionCount = stats.size() - successCount;
        System.out.println("\n===== Coverage Summary Report =====");
        System.out.println("Total Patterns: " + stats.size());
        System.out.println("Successes:      " + successCount);
        System.out.println("Exceptions:     " + exceptionCount);
        Map<String, Long> exceptionTypes = stats.stream()
            .filter(s -> "exception".equals(s.get("result")))
            .collect(Collectors.groupingBy(s -> String.valueOf(s.get("exceptionType")), Collectors.counting()));
        System.out.println("Exception Types:");
        for (Map.Entry<String, Long> entry : exceptionTypes.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("==================================\n");
    }

    /**
     * 打印详细的每个pattern的结果。
     */
    public void printDetailedReport(int limit) {
        System.out.println("===== Detailed Pattern Results (limit " + limit + ") =====");
        for (int i = 0; i < Math.min(limit, stats.size()); i++) {
            Map<String, Object> stat = stats.get(i);
            System.out.println("Pattern " + i + ": " + stat);
        }
        System.out.println("============================================\n");
    }

    /**
     * 以树结构展示所有pattern的覆盖情况。
     * 假设pattern为List<String>，每一层代表一个API调用的异常类型。
     */
    public void printCoverageTree() {
        TreeNode root = new TreeNode("root");
        for (Map<String, Object> stat : stats) {
            @SuppressWarnings("unchecked")
            List<String> pattern = (List<String>) stat.get("pattern");
            String result = String.valueOf(stat.get("result"));
            String label = result;
            if ("exception".equals(result)) {
                label += ":" + stat.get("exceptionType");
            }
            root.addPattern(pattern, label);
        }
        System.out.println("===== Pattern Coverage Tree =====");
        root.print(0);
        System.out.println("================================\n");
    }

    /**
     * 内部树节点类。
     */
    private static class TreeNode {
        String value;
        Map<String, TreeNode> children = new LinkedHashMap<>();
        String leafLabel = null;
        TreeNode(String value) { this.value = value; }
        void addPattern(List<String> pattern, String label) {
            addPattern(pattern, 0, label);
        }
        void addPattern(List<String> pattern, int idx, String label) {
            if (idx == pattern.size()) {
                this.leafLabel = label;
                return;
            }
            String key = pattern.get(idx);
            children.putIfAbsent(key, new TreeNode(key));
            children.get(key).addPattern(pattern, idx + 1, label);
        }
        void print(int depth) {
            String indent = "  ".repeat(depth);
            System.out.print(indent + value);
            if (leafLabel != null) {
                System.out.print(" => [" + leafLabel + "]");
            }
            System.out.println();
            for (TreeNode child : children.values()) {
                child.print(depth + 1);
            }
        }
    }
}
