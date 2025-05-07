package edu.unl.exceptionamplifier.builder;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import io.github.cdimascio.dotenv.Dotenv; // 自动.env加载

import com.alibaba.fastjson.JSONArray;

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
    /**
     * 生成所有可能的Mocking Pattern组合（支持 LLM 或回溯法）
     * @param apiCalls API调用序列
     * @param exceptionTypes 支持的异常类型
     * @param useLLM 是否用LLM（如阿里云百炼）生成
     * @return 所有组合，每个组合是一个对应于apiCalls的异常/normal列表
     */
    public List<List<String>> generateMockingPatterns(List<String> apiCalls, List<String> exceptionTypes, boolean useLLM) {
        if (apiCalls == null || apiCalls.isEmpty()) return new ArrayList<>();
        if (useLLM) {
            String prompt = "已知API调用序列：" + apiCalls +
                    "\n支持的异常类型：" + exceptionTypes +
                    "\n请为每个API调用点生成所有可能的Mocking Pattern组合，每个组合是一个长度为" + apiCalls.size() +
                    "的列表，元素为normal或异常类型，返回JSON格式的Java List<List<String>>，不要有多余解释。";
            String llmResponse = callLLM(prompt);
            return parseMockPatterns(llmResponse);
        } else {
            List<List<String>> results = new ArrayList<>();
            backtrack(apiCalls.size(), exceptionTypes, new ArrayList<>(), results);
            return results;
        }
    }

    // 保持兼容性，原有方法调用新方法（默认不用LLM）
    public List<List<String>> generateMockingPatterns(List<String> apiCalls, List<String> exceptionTypes) {
        return generateMockingPatterns(apiCalls, exceptionTypes, false);
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
