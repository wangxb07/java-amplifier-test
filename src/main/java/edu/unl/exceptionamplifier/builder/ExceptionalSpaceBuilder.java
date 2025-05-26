package edu.unl.exceptionamplifier.builder;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import io.github.cdimascio.dotenv.Dotenv; // 自动.env加载

import com.alibaba.fastjson.JSONArray;

public class ExceptionalSpaceBuilder {
    // Define an enum for generation strategies
    public static enum PatternGenerationStrategy {
        EXHAUSTIVE, // 穷尽式
        HIGH_RISK_SELECTIVE, // 由HighRisk参与的选择性生成
        LLM_BASED, // LLM参与的所有可能的模拟模式组合
        DEFAULT_RISK_BASED // 默认的基于风险的生成（结合了单一异常和高风险API对）
    }

    private final Set<String> exceptionSpace = new HashSet<>();
    private final Map<String, Double> apiRiskScores = new HashMap<>(); // Example: apiRiskScores.put("api1", 1.5);

    public ExceptionalSpaceBuilder() {
    }

    public void addException(String exception) {
        exceptionSpace.add(exception);
    }

    public Set<String> getExceptionSpace() {
        return new HashSet<>(exceptionSpace);
    }

    // Method to set API risk scores, useful for HIGH_RISK_SELECTIVE
    public void setApiRiskScore(String apiCall, double score) {
        this.apiRiskScores.put(apiCall, score);
    }

    /**
     * 生成基于风险的测试用例 (can be used for DEFAULT_RISK_BASED and as a basis for HIGH_RISK_SELECTIVE)
     */
    public List<List<String>> generateRiskBasedPatterns(List<String> apiCalls,
                                                      List<String> exceptionTypes) {
        List<List<String>> patterns = new ArrayList<>();
        if (apiCalls == null || apiCalls.isEmpty()) {
            return patterns;
        }
//        List<String> allPossibleStates = new ArrayList<>(exceptionTypes);
//        allPossibleStates.add("normal");

        // 1. Normal case (all calls are normal)
        patterns.add(Collections.nCopies(apiCalls.size(), "normal"));

        // 2. Single exception patterns (one API call fails, others are normal)
        for (int i = 0; i < apiCalls.size(); i++) {
            for (String exception : exceptionTypes) {
                List<String> pattern = new ArrayList<>(Collections.nCopies(apiCalls.size(), "normal"));
                pattern.set(i, exception);
                patterns.add(pattern);
            }
        }

        // 3. High-risk combinations (based on apiRiskScores)
        // This part can be enhanced for a more sophisticated HIGH_RISK_SELECTIVE mode
        generateHighRiskPatterns(apiCalls, exceptionTypes, patterns);

        return patterns;
    }

    /**
     * 生成高风险组合的测试用例
     * This is a helper for generateRiskBasedPatterns and can be expanded for HIGH_RISK_SELECTIVE
     */
    private void generateHighRiskPatterns(List<String> apiCalls,
                                        List<String> exceptionTypes,
                                        List<List<String>> patterns) {
        // 识别高风险API组合
        List<String> highRiskApis = new ArrayList<>();
        // Example threshold, can be configurable
        double riskThreshold = 1.2;
        for (Map.Entry<String, Double> entry : apiRiskScores.entrySet()) {
            if (entry.getValue() > riskThreshold && apiCalls.contains(entry.getKey())) {
                highRiskApis.add(entry.getKey());
            }
        }

        // 为高风险API生成异常组合 (currently pairs of high-risk APIs)
        // This logic can be made more sophisticated for HIGH_RISK_SELECTIVE
        if (highRiskApis.size() >= 2) {
            for (int i = 0; i < highRiskApis.size(); i++) {
                for (int j = i + 1; j < highRiskApis.size(); j++) {
                    String highRiskApi1 = highRiskApis.get(i);
                    String highRiskApi2 = highRiskApis.get(j);
                    int index1 = apiCalls.indexOf(highRiskApi1);
                    int index2 = apiCalls.indexOf(highRiskApi2);

                    if (index1 == -1 || index2 == -1) continue; // Should not happen if highRiskApis are derived from apiCalls

                    for (String ex1 : exceptionTypes) {
                        for (String ex2 : exceptionTypes) {
                            List<String> pattern = new ArrayList<>(Collections.nCopies(apiCalls.size(), "normal"));
                            pattern.set(index1, ex1);
                            pattern.set(index2, ex2);
                            patterns.add(pattern);
                        }
                    }
                }
            }
        }
        // Consider adding patterns for single high-risk APIs having an exception if not covered
        // or if HIGH_RISK_SELECTIVE requires more focused single high-risk API patterns.
    }

    /**
     * 新增：生成穷尽式模式
     * Generates exhaustive patterns for the first k API calls.
     * For N calls and M exception types (+ "normal"), generates (M+1)^k patterns for the first k calls.
     * Remaining N-k calls are set to "normal".
     *
     * @param apiCalls List of API call identifiers.
     * @param exceptionTypes List of possible exception types.
     * @param k The number of initial API calls to generate exhaustive patterns for. If k > N, it uses N.
     * @return A list of generated mock patterns.
     */
    public List<List<String>> generateExhaustivePatterns(List<String> apiCalls, List<String> exceptionTypes, int k) {
        List<List<String>> patterns = new ArrayList<>();
        if (apiCalls == null || apiCalls.isEmpty()) {
            return patterns;
        }

        int n = apiCalls.size();
        int numCallsToVary = Math.min(k, n);

        List<String> possibleStates = new ArrayList<>(exceptionTypes);
        possibleStates.add("normal"); // Add "normal" state

        int numStates = possibleStates.size();
        long totalPatternsToGenerate = (long) Math.pow(numStates, numCallsToVary);

        // To prevent generating an excessive number of patterns that might lead to memory issues.
        // This limit can be adjusted.
        if (totalPatternsToGenerate > 100000) { // Example limit
            System.err.println("Warning: Exhaustive pattern generation for k=" + numCallsToVary +
                               " would create " + totalPatternsToGenerate +
                               " patterns, which exceeds the limit. Returning empty list.");
            // Or, alternatively, could throw an exception or return a subset.
            return patterns;
        }

        // Helper for recursive generation
        generateExhaustiveRecursive(apiCalls, possibleStates, numCallsToVary, n, 0, new ArrayList<>(Collections.nCopies(n, "normal")), patterns);

        return patterns;
    }

    private void generateExhaustiveRecursive(List<String> apiCalls, List<String> possibleStates,
                                             int k, int n, int currentIndex,
                                             List<String> currentPattern, List<List<String>> allPatterns) {
        if (currentIndex == k) {
            // First k calls have been set, the rest are 'normal' (already initialized)
            allPatterns.add(new ArrayList<>(currentPattern));
            return;
        }

        for (String state : possibleStates) {
            currentPattern.set(currentIndex, state);
            generateExhaustiveRecursive(apiCalls, possibleStates, k, n, currentIndex + 1, currentPattern, allPatterns);
        }
    }


    // Refactored main generation method
    public List<List<String>> generateMockingPatterns(List<String> apiCalls,
                                                    List<String> exceptionTypes,
                                                    PatternGenerationStrategy strategy,
                                                    int kForExhaustive) { // kForExhaustive is only used for EXHAUSTIVE strategy
        switch (strategy) {
            case EXHAUSTIVE:
                if (kForExhaustive <= 0) {
                    // Default to varying all calls if k is not specified or invalid for exhaustive
                    return generateExhaustivePatterns(apiCalls, exceptionTypes, apiCalls.size());
                }
                return generateExhaustivePatterns(apiCalls, exceptionTypes, kForExhaustive);
            case HIGH_RISK_SELECTIVE:
                // For HIGH_RISK_SELECTIVE, we might want a more tailored approach.
                // For now, it can use generateRiskBasedPatterns, which includes high-risk pairs.
                // This can be expanded based on more specific requirements for "HighRisk参与的选择性生成".
                // For example, you might want to prompt the user for which specific high-risk APIs to focus on,
                // or implement a more advanced selection logic.
                System.out.println("Note: HIGH_RISK_SELECTIVE currently uses the default risk-based pattern generation. " +
                                   "Further refinement might be needed for specific selective logic.");
                return generateRiskBasedPatterns(apiCalls, exceptionTypes);
            case LLM_BASED:
                return generateMockingPatternsWithLLM(apiCalls, exceptionTypes);
            case DEFAULT_RISK_BASED:
            default:
                return generateRiskBasedPatterns(apiCalls, exceptionTypes);
        }
    }

    // Overloaded method for convenience, defaulting to DEFAULT_RISK_BASED
    public List<List<String>> generateMockingPatterns(List<String> apiCalls, List<String> exceptionTypes) {
        return generateMockingPatterns(apiCalls, exceptionTypes, PatternGenerationStrategy.DEFAULT_RISK_BASED, apiCalls.size());
    }

    // Overloaded method to maintain compatibility with existing boolean useLLM flag
    public List<List<String>> generateMockingPatterns(List<String> apiCalls,
                                                    List<String> exceptionTypes,
                                                    boolean useLLM) {
        if (useLLM) {
            return generateMockingPatterns(apiCalls, exceptionTypes, PatternGenerationStrategy.LLM_BASED, apiCalls.size());
        }
        return generateMockingPatterns(apiCalls, exceptionTypes, PatternGenerationStrategy.DEFAULT_RISK_BASED, apiCalls.size());
    }

    private List<List<String>> generateMockingPatternsWithLLM(List<String> apiCalls,
                                                            List<String> exceptionTypes) {
        // Construct a more detailed prompt for the LLM
        String apiCallString = String.join(", ", apiCalls);
        String exceptionTypeString = String.join(", ", exceptionTypes);

        // Enhanced prompt for LLM
        String prompt = String.format(
            "Consider a sequence of %d API calls: [%s]. " +
            "The possible states for each API call are 'normal' or one of the following exception types: [%s]. " +
            "Generate a diverse and comprehensive set of mock patterns representing different scenarios of these API calls failing or succeeding. " +
            "Each pattern should be a list of states, one for each API call in the sequence. " +
            "Prioritize scenarios that are non-trivial, including multiple failures, and sequences of failures. " +
            "Return the result as a JSON formatted Java List<List<String>>. For example: [[\"normal\", \"ExceptionA\"], [\"ExceptionB\", \"normal\"]]. " +
            "Do not include any explanations, comments, or natural language outside of the JSON array itself.",
            apiCalls.size(), apiCallString, exceptionTypeString
        );

        String llmResponse = callLLM(prompt);
        return parseMockPatterns(llmResponse);
    }

    /**
     * 调用OpenAI官方或兼容API（如阿里云 DashScope）。
     * 支持通过环境变量或 .env 文件配置 apiKey 和 baseUrl。
     * 推荐在 .env 文件中设置：
     *   OPENAI_API_KEY=你的APIKey
     *   OPENAI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
     */
    private String callLLM(String prompt) {
        try {
            // 优先用系统环境变量，其次加载 .env
            String apiKey = System.getenv("DASHSCOPE_API_KEY");
            String baseUrl = System.getenv("DASHSCOPE_BASE_URL");
            if ((apiKey == null || apiKey.isEmpty()) || (baseUrl == null || baseUrl.isEmpty())) {
                Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
                if (apiKey == null || apiKey.isEmpty()) {
                    apiKey = dotenv.get("DASHSCOPE_API_KEY");
                }
                if (baseUrl == null || baseUrl.isEmpty()) {
                    baseUrl = dotenv.get("DASHSCOPE_BASE_URL");
                }
            }
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("DASHSCOPE_API_KEY not set in environment or .env file");
            }
            if (baseUrl == null || baseUrl.isEmpty()) {
                // 默认用openai官方
                baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
            }
            OpenAIClient client = OpenAIOkHttpClient.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .build();
            // 强化 system prompt
String systemPrompt = "你是一个只返回 JSON 的 API。所有输出必须是合法的 JSON 数组（如 [[\"normal\",\"exception\"],[\"exception\",\"normal\"]]），不要有任何解释、注释或自然语言说明。";
// 强化用户 prompt
String finalPrompt = prompt + "\n请严格只返回合法的 JSON 数组，不要有任何多余的解释、注释或自然语言说明。";
ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
        .addSystemMessage(systemPrompt)
        .addUserMessage(finalPrompt)
        .model("qwen-plus")
        .build();
            ChatCompletion chatCompletion = client.chat().completions().create(params);
            if (chatCompletion.choices() != null && !chatCompletion.choices().isEmpty()) {
                return chatCompletion.choices().get(0).message().content().orElse("");
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 解析LLM返回的JSON格式List<List<String>>
     */
    private List<List<String>> parseMockPatterns(String llmResponse) {
        List<List<String>> patterns = new ArrayList<>();
        if (llmResponse == null || llmResponse.isEmpty()) return patterns;
        try {
            // 预处理：只提取第一个合法 JSON 数组
            String json = llmResponse;
            int startIdx = json.indexOf('[');
            int endIdx = json.lastIndexOf(']');
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                json = json.substring(startIdx, endIdx + 1);
            }
            JSONArray arr = JSONArray.parseArray(json);
            for (int i = 0; i < arr.size(); i++) {
                List<String> inner = new ArrayList<>();
                JSONArray innerArr = arr.getJSONArray(i);
                for (int j = 0; j < innerArr.size(); j++) {
                    inner.add(innerArr.getString(j));
                }
                patterns.add(inner);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return patterns;
    }
}
