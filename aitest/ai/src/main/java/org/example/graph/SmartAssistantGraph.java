package org.example.graph;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final VectorStore vectorStore;
    QuestionAnswerAdvisor ragAdvisor;

    public SmartAssistantGraph(ChatClient.Builder builder, VectorStore vectorStore, QuestionAnswerAdvisor ragAdvisor) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
        this.ragAdvisor = ragAdvisor;
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
                "ragContext", new ReplaceStrategy(),
                "messages", new AppendStrategy()
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

            String reply = chatClient.prompt(prompt).advisors(ragAdvisor).call().content();
            return Map.of("output", reply);
        };
    }

    /**
     * 隐私拦截
     */
    @Bean
    public NodeAction privateNode() {
        return state -> Map.of("output", "我是一个AI助手，没有个人隐私信息哦。如果你想聊天或问问题，我很乐意帮忙！");
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
     * RAG 检索节点
     */
    @Bean
    public NodeAction ragNode() {
        return state -> {
            String input = state.value("input", "").toString();
            log.info("RAG检索: query={}", input);

            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(input)
                            .topK(5) //  只取最相关的 3 条
                            .similarityThreshold(0.7) // ← 过滤低相关度
                            .build()
            );

            if (docs.isEmpty()) {
                log.info("RAG检索: 未找到相关文档");
                return Map.of("ragContext", "");
            }

            String context = docs.stream()
                    .map(doc -> doc.getText())
                    .collect(Collectors.joining("\n\n"));

            log.info("RAG检索: 找到{}条相关文档", docs.size());
            return Map.of("ragContext", context);
        };
    }

    /**
     * 历史记忆
     */
    @Bean
    public NodeAction memoryNode() {
        return state -> {
            // 从 state 读取历史消息
            @SuppressWarnings("unchecked")
            List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());
            List<Message> result;
            if (messages.size() <= 10) {
                // 消息不多，直接保留 + 追加当前
                result = new ArrayList<>(messages);
            } else {
                // 消息太多，用 LLM 压缩旧消息为摘要
                List<Message> recentMessages = messages.subList(messages.size() - 8, messages.size());
                List<Message> oldMessages = messages.subList(1, messages.size() - 8); // 跳过首条 SystemMessage

                // 把旧消息拼成文本，让 LLM 生成摘要
                String oldText = oldMessages.stream()
                        .map(m -> (m instanceof UserMessage ? "用户" : "助手") + ": " + m.getText())
                        .collect(Collectors.joining("\n"));

                String summaryPrompt = """
                        请将以下对话历史压缩成一段简洁的摘要，保留关键事实和用户偏好，不超过100字：
                        %s
                        直接返回摘要文本，不要加其他说明。
                        """.formatted(oldText);

                String summary = chatClient.prompt(summaryPrompt).call().content();
                log.info("历史记忆: 压缩{}条旧消息为摘要", oldMessages.size());

                result = new ArrayList<>();
                result.add(messages.get(0)); // 保留首条 SystemMessage
                result.add(new SystemMessage("## 之前对话摘要:\n" + summary)); // 插入摘要
                result.addAll(recentMessages); // 最近 8 条原文
            }

            log.info("历史记忆: 最终{}条消息", result.size());
            return Map.of("messages", result);
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
            String ragContext = state.value("ragContext", "").toString();

            @SuppressWarnings("unchecked")
            List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());

            StringBuilder systemContent = new StringBuilder(persona);
            if (!ragContext.isEmpty()) {
                systemContent.append("\n\n【参考知识】\n").append(ragContext);
                systemContent.append("\n\n请优先基于上述参考知识回答。如果知识与用户历史对话冲突，以参考知识为准。");
            }
            systemContent.append("\n\n如果参考知识中没有相关信息，请明确告知用户你不知道，不要编造答案。");

            // 2. 分层构建消息列表
            List<Message> allMessages = new ArrayList<>();

            // 第一层：SystemMessage（人设 + RAG）
            allMessages.add(new SystemMessage(systemContent.toString()));

            // 第二层：历史对话（只保留 UserMessage 和 AssistantMessage，跳过中间的摘要 SystemMessage）
            for (Message msg : messages) {
                if (msg instanceof UserMessage || msg instanceof AssistantMessage) {
                    allMessages.add(msg);
                }
                // 摘要 SystemMessage 的内容已经融入上一层的 systemContent，这里不再重复添加
            }

            // 第三层：当前轮用户问题（单独作为最后一条 UserMessage，明确"这是本轮要回答的"）
            allMessages.add(new UserMessage(input));

            String answer = chatClient.prompt()
                    .messages(allMessages)
                    .advisors(ragAdvisor)
                    .call()
                    .content();
            log.info("qaNode: ragContext长度={}, 内容={}", ragContext.length(), ragContext);
            log.info("QA回答: {}", answer);
            return Map.of(
                    "output", answer,
                    "messages", List.of(new AssistantMessage(answer))
            );
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
        graph.addNode("rag", node_async(ragNode()));
        graph.addNode("memory", node_async(memoryNode()));
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
                        "chat", "rag",
                        "private", "private",
                        "persona", "persona",
                        "qa", "rag"
                )
        );

        graph.addEdge("private", "outputSafety");
        graph.addEdge("persona", "outputSafety");
        graph.addEdge("rag", "memory");
        graph.addEdge("qa", "outputSafety");
        graph.addEdge("chat", "outputSafety");
        graph.addEdge("outputSafety", END);

        graph.addConditionalEdges("memory",
                edge_async(state -> state.value("intent", "qa").toString()),
                Map.of(
                        "chat", "chat",
                        "qa", "qa"
                )
        );
        return graph.compile(CompileConfig.builder()
                .saverConfig(saverConfig)
                .build());
    }
}