package edu.unl.exceptionamplifier.util;

import java.util.*;
import java.util.stream.Collectors;

public class CoverageStatsReporter {
    private final List<CoverageStat> stats = new ArrayList<>();
    public void addStat(String testName, String pattern, boolean covered) {
        stats.add(new CoverageStat(testName, pattern, covered));
    }
    public void printSummaryReport() {
        long covered = stats.stream().filter(CoverageStat::isCovered).count();
        System.out.printf("\n[覆盖率统计] 共 %d 条, 覆盖 %d 条, 覆盖率 %.2f%%\n", stats.size(), covered, stats.size() == 0 ? 0.0 : (covered * 100.0 / stats.size()));
    }
    public void printDetailReport() {
        System.out.println("\n[详细覆盖率结果]");
        for (CoverageStat stat : stats) {
            System.out.printf("%s\t%s\t%s\n", stat.getTestName(), stat.getPattern(), stat.isCovered() ? "✔" : "✘");
        }
    }
    public void printCoverageTree() {
        System.out.println("\n[覆盖率树结构]");
        TreeNode root = new TreeNode("ROOT");
        for (CoverageStat stat : stats) {
            List<String> patternPath = Arrays.asList(stat.getPattern().split("\\."));
            root.addPattern(patternPath, stat.isCovered() ? "✔" : "✘");
        }
        root.print(0);
    }
    private static class CoverageStat {
        private final String testName;
        private final String pattern;
        private final boolean covered;
        public CoverageStat(String testName, String pattern, boolean covered) {
            this.testName = testName;
            this.pattern = pattern;
            this.covered = covered;
        }
        public String getTestName() { return testName; }
        public String getPattern() { return pattern; }
        public boolean isCovered() { return covered; }
    }
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
            StringBuilder indentBuilder = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                indentBuilder.append("  ");
            }
            String indent = indentBuilder.toString();
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
