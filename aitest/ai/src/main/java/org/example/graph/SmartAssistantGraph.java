package org.example.graph;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * @author huoqi
 */
@Slf4j
@Configuration
public class SmartAssistantGraph {

    private final ChatClient chatClient;

    public SmartAssistantGraph(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // ==================== 状态策略 ====================

    @Bean
    public KeyStrategyFactory keyStrategyFactory() {
        return () -> Map.of(
                "input", new ReplaceStrategy(),
                "intent", new ReplaceStrategy(),
                "output", new ReplaceStrategy(),
                "persona", new ReplaceStrategy(),
                "safe", new ReplaceStrategy(),
                "toolResult", new ReplaceStrategy()
        );
    }

    // ==================== 节点定义 ====================

    /**
     * 输入安全检查
     * 拦截敏感/违规内容
     */
    @Bean
    public NodeAction safetyCheckNode() {
        return state -> {
            String input = state.value("input", "").toString();
            List<String> badWords = List.of("色情", "暴力");
            boolean safe = badWords.stream().noneMatch(input::contains);
            log.info("安全检查: input={}, safe={}", input, safe);

            if (!safe) {
                // 输入有敏感词，直接设置输出并结束流程
                return Map.of(
                        "safe", false,
                        "output", "请正能量"
                );
            }
            return Map.of("safe", true);
        };
    }

    /**
     * 意图识别
     * 调用 LLM 判断用户意图
     */
    @Bean
    public NodeAction intentNode() {
        return state -> {
            String input = state.value("input", "").toString();
            String prompt = String.format("""
                    你是一个意图识别助手。请分析用户输入的意图，只返回一个JSON对象，不要返回其他内容。
                    意图类型：
                    - chat: 打招呼
                    - private: 询问个人隐私，如 "你叫什么"、"你几岁"、"你家在哪"
                    - persona: 切换人设，如 "你现在是个翻译"、"扮演面试官"
                    - qa: 其他通用问答
                    
                    用户输入：%s
                    
                    返回格式示例：{"intent":"qa"}
                    """, input);

            String json = chatClient.prompt(prompt).call().content();
            log.info("意图识别结果: {}", json);

            // 简单解析 JSON
            String intent = "qa"; // 默认
            if (json.contains("\"intent\"")) {
                int start = json.indexOf("\"intent\"") + 10;
                int end = json.indexOf("\"", start);
                if (end > start) {
                    intent = json.substring(start, end);
                }
            }
            return Map.of("intent", intent);
        };
    }

    /**
     * 打招呼
     */
    @Bean
    public NodeAction chatNode() {
        return state -> {
            String input = state.value("input", String.class).orElse("");

            String prompt = """
                    你是一个热情可爱的聊天助手。请用轻松活泼的语气回复用户，回复中必须包含表情符号(emoji)和颜文字。
                    用户说：%s
                    请直接回复内容，不要加其他说明。
                    """.formatted(input);

            String reply = chatClient.prompt(prompt).call().content();
            return Map.of("output", reply);
        };
    }

    /**
     * 隐私拦截
     */
    @Bean
    public NodeAction privateNode() {
        return state -> Map.of("output", "我是一个AI助手，没有个人隐私信息哦。");
    }

    /**
     * 人设切换
     */
    @Bean
    public NodeAction personaNode() {
        return state -> {
            String input = state.value("input", "").toString();
            String persona = "你是一个乐于助人的AI助手。";
            if (input.contains("翻译")) {
                persona = "你是一个专业的翻译助手，只负责翻译，不回答其他问题。";
            } else if (input.contains("面试官")) {
                persona = "你是一个资深技术面试官，负责模拟面试。";
            } else if (input.contains("老师")) {
                persona = "你是一个耐心的老师，负责解答学生问题。";
            }
            return Map.of(
                    "persona", persona,
                    "output", "好的，" + persona + " 请开始吧。"
            );
        };
    }

    /**
     * 通用问答
     */
    @Bean
    public NodeAction qaNode() {
        return state -> {
            String input = state.value("input", "").toString();
            String persona = state.value("persona", "你是一个乐于助人的AI助手。").toString();

            StringBuilder prompt = new StringBuilder();
            prompt.append("人设: ").append(persona).append("\n");
            prompt.append("用户问题: ").append(input).append("\n");
            prompt.append("请回答:");

            String answer = chatClient.prompt(prompt.toString()).call().content();
            return Map.of("output", answer);
        };
    }

    /**
     * 输出安全检查
     */
    @Bean
    public NodeAction outputSafetyNode() {
        return state -> {
            String output = state.value("output", "").toString();
            List<String> badWords = List.of("色情", "暴力", "赌博", "毒品");

            String maskedOutput = output;
            for (String word : badWords) {
                if (maskedOutput.contains(word)) {
                    maskedOutput = maskedOutput.replace(word, "[" + word + "]");
                }
            }

            return Map.of("output", maskedOutput);
        };
    }

    // ==================== 编译 Graph ====================

    @Bean
    public CompiledGraph compiledGraph() throws Exception {
        StateGraph graph = new StateGraph(keyStrategyFactory());
        // MemorySaver（开发环境）或 RedisSaver（生产环境）
        MemorySaver checkpointer = new MemorySaver();
        SaverConfig saverConfig = SaverConfig.builder()
                .register(checkpointer)
                .build();

        // 添加节点
        graph.addNode("safetyCheck", node_async(safetyCheckNode()));
        graph.addNode("intent", node_async(intentNode()));
        graph.addNode("chat", node_async(chatNode()));
        graph.addNode("private", node_async(privateNode()));
        graph.addNode("persona", node_async(personaNode()));
        graph.addNode("qa", node_async(qaNode()));
        graph.addNode("outputSafety", node_async(outputSafetyNode()));

        // 连接边
        graph.addEdge(START, "safetyCheck");

        // 安全检查后的条件路由
        graph.addConditionalEdges("safetyCheck",
                edge_async(state -> {
                    boolean safe = (Boolean) state.value("safe").orElse(false);
                    return safe ? "intent" : "blocked";
                }),
                Map.of(
                        "intent", "intent",
                        "blocked", END
                )
        );

        // 意图识别后的条件路由
        graph.addConditionalEdges("intent",
                edge_async(state -> state.value("intent", "qa").toString()),
                Map.of(
                        "chat", "chat",
                        "private", "private",
                        "persona", "persona",
                        "qa", "qa"
                )
        );

        graph.addEdge("chat", "outputSafety");
        graph.addEdge("private", "outputSafety");
        graph.addEdge("persona", "outputSafety");
        graph.addEdge("qa", "outputSafety");

        graph.addEdge("outputSafety", END);

        return graph.compile(CompileConfig.builder()
                .saverConfig(saverConfig)
                .build());
    }
}