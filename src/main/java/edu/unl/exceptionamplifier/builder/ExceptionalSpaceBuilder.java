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
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import io.github.cdimascio.dotenv.Dotenv; // 自动.env加载

import com.alibaba.fastjson.JSONArray;

public class ExceptionalSpaceBuilder {
    private final Set<String> exceptionSpace = new HashSet<>();
    private final Map<String, Double> exceptionWeights = new HashMap<>();
    private final Map<String, Double> apiRiskScores = new HashMap<>();
    private double coverageThreshold = 0.8; // 默认覆盖率阈值

    public ExceptionalSpaceBuilder() {
        // 初始化异常权重
        exceptionWeights.put("IOException", 0.8);
        exceptionWeights.put("TimeoutException", 0.7);
        exceptionWeights.put("RemoteApiException", 0.9);
        exceptionWeights.put("PositionNotEnoughException", 0.6);
        exceptionWeights.put("InsufficientBalanceException", 0.5);
    }

    public void setCoverageThreshold(double threshold) {
        this.coverageThreshold = threshold;
    }

    public void addException(String exception) {
        exceptionSpace.add(exception);
    }

    public Set<String> getExceptionSpace() {
        return new HashSet<>(exceptionSpace);
    }

    /**
     * 计算API调用的风险分数
     */
    private Map<String, Double> calculateApiRiskScores(List<String> apiCalls) {
        Map<String, Double> scores = new HashMap<>();
        for (String apiCall : apiCalls) {
            // 基于API名称和调用位置计算风险分数
            double score = 1.0;
            if (apiCall.contains("buy") || apiCall.contains("sell")) {
                score *= 1.5; // 交易相关API风险更高
            }
            if (apiCall.contains("Database")) {
                score *= 1.2; // 数据库操作风险较高
            }
            scores.put(apiCall, score);
        }
        return scores;
    }

    /**
     * 生成基于风险的测试用例
     */
    public List<List<String>> generateRiskBasedPatterns(List<String> apiCalls, 
                                                      List<String> exceptionTypes,
                                                      double coverageThreshold) {
        this.coverageThreshold = coverageThreshold;
        List<List<String>> patterns = new ArrayList<>();
        apiRiskScores.putAll(calculateApiRiskScores(apiCalls));
        
        // 生成初始测试用例
        patterns.add(Collections.nCopies(apiCalls.size(), "normal")); // 正常情况
        
        // 为每个API生成单个异常的情况
        for (String apiCall : apiCalls) {
            for (String exception : exceptionTypes) {
                List<String> pattern = new ArrayList<>(Collections.nCopies(apiCalls.size(), "normal"));
                pattern.set(apiCalls.indexOf(apiCall), exception);
                patterns.add(pattern);
            }
        }
        
        // 生成高风险组合
        generateHighRiskPatterns(apiCalls, exceptionTypes, patterns);
        
        return patterns;
    }

    /**
     * 生成高风险组合的测试用例
     */
    private void generateHighRiskPatterns(List<String> apiCalls, 
                                        List<String> exceptionTypes,
                                        List<List<String>> patterns) {
        // 识别高风险API组合
        List<String> highRiskApis = new ArrayList<>();
        for (Map.Entry<String, Double> entry : apiRiskScores.entrySet()) {
            if (entry.getValue() > 1.2) { // 风险分数阈值
                highRiskApis.add(entry.getKey());
            }
        }
        
        // 为高风险API生成异常组合
        for (int i = 0; i < highRiskApis.size(); i++) {
            for (int j = i + 1; j < highRiskApis.size(); j++) {
                for (String ex1 : exceptionTypes) {
                    for (String ex2 : exceptionTypes) {
                        List<String> pattern = new ArrayList<>(Collections.nCopies(apiCalls.size(), "normal"));
                        pattern.set(apiCalls.indexOf(highRiskApis.get(i)), ex1);
                        pattern.set(apiCalls.indexOf(highRiskApis.get(j)), ex2);
                        patterns.add(pattern);
                    }
                }
            }
        }
    }

    // 保持原有方法以兼容现有代码
    public List<List<String>> generateMockingPatterns(List<String> apiCalls, List<String> exceptionTypes) {
        return generateRiskBasedPatterns(apiCalls, exceptionTypes, 0.8);
    }

    public List<List<String>> generateMockingPatterns(List<String> apiCalls, 
                                                    List<String> exceptionTypes, 
                                                    boolean useLLM) {
        if (useLLM) {
            return generateMockingPatternsWithLLM(apiCalls, exceptionTypes);
        }
        return generateRiskBasedPatterns(apiCalls, exceptionTypes, 0.8);
    }

    private List<List<String>> generateMockingPatternsWithLLM(List<String> apiCalls, 
                                                            List<String> exceptionTypes) {
        // 使用LLM生成测试用例的逻辑保持不变
        String prompt = "已知API调用序列：" + apiCalls +
                "\n支持的异常类型：" + exceptionTypes +
                "\n请为每个API调用点生成所有可能的Mocking Pattern组合，每个组合是一个长度为" + apiCalls.size() +
                "的列表，元素为normal或异常类型，返回JSON格式的Java List<List<String>>，不要有多余解释。";
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
