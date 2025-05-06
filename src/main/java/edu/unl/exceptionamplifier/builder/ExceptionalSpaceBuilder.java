package edu.unl.exceptionamplifier.builder;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

public class ExceptionalSpaceBuilder {
    private final Set<String> exceptionSpace = new HashSet<>();

    public void addException(String exception) {
        exceptionSpace.add(exception);
    }

    public Set<String> getExceptionSpace() {
        return new HashSet<>(exceptionSpace);
    }

    /**
     * 生成所有可能的Mocking Pattern组合，例如 [normal, exception, normal]
     * @param apiCalls API调用序列
     * @param exceptionTypes 支持的异常类型
     * @return 所有组合，每个组合是一个对应于apiCalls的异常/normal列表
     */
    public List<List<String>> generateMockingPatterns(List<String> apiCalls, List<String> exceptionTypes) {
        List<List<String>> results = new ArrayList<>();
        if (apiCalls == null || apiCalls.isEmpty()) return results;
        backtrack(apiCalls.size(), exceptionTypes, new ArrayList<>(), results);
        return results;
    }

    private void backtrack(int n, List<String> exceptionTypes, List<String> current, List<List<String>> results) {
        if (current.size() == n) {
            results.add(new ArrayList<>(current));
            return;
        }
        // normal
        current.add("normal");
        backtrack(n, exceptionTypes, current, results);
        current.remove(current.size() - 1);
        // each exception
        for (String ex : exceptionTypes) {
            current.add(ex);
            backtrack(n, exceptionTypes, current, results);
            current.remove(current.size() - 1);
        }
    }
}
