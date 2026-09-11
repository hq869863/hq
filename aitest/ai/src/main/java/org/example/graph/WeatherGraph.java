package org.example.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.example.config.AIOpsToolAssembly;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * @author huoqi
 */
@Slf4j
@Configuration
public class WeatherGraph {


    private final ChatClient dashscopeChatClient;
    private final ReactAgent reactAgent;
    private final ChatMemory chatMemory;

    public WeatherGraph(
            ChatModel dashscopeChatModel,
            @Qualifier("dashscopeChatClient") ChatClient dashscopeChatClient,
            AIOpsToolAssembly toolAssembly,
//            RedissonClient redissonClient,
            ChatMemory chatMemory,
            @Value("${app.ai.default-system-prompt}") String systemPrompt) {

        this.dashscopeChatClient = dashscopeChatClient;
        this.chatMemory = chatMemory;

        // 🚀 1. 构建带 Redis 记忆的 ReactAgent
        this.reactAgent = ReactAgent.builder()
                .name("weather_agent_node")
                .model(dashscopeChatModel)
                .tools(toolAssembly.getAllTools())
                .instruction(systemPrompt)
                .outputKey("agent_response")
//                .saver(RedisSaver.builder().redisson(redissonClient).build())
                .build();
    }

    @Bean
    public CompiledGraph weatherGraph1() throws GraphStateException {
        // 2. 定义状态更新策略
        KeyStrategyFactory keyStrategyFactory = () -> Map.of(
                "user_input", new ReplaceStrategy(),
                "thread_id", new ReplaceStrategy(),
                "intent", new ReplaceStrategy(),
                "chat_response", new ReplaceStrategy(),
                "agent_response", new ReplaceStrategy()
        );

        StateGraph stateGraph = new StateGraph("weather_workflow", keyStrategyFactory);

        // 3. 节点1：意图识别与闲聊处理（带 ChatMemory）
        stateGraph.addNode("intent_node", node_async(state -> {
            String userQuery = (String) state.value("user_input").orElse("");
            // 🚀 从 State 中获取 threadId，用于隔离不同用户的闲聊记忆
            String threadId = (String) state.value("thread_id").orElse("default_session");

            log.info("🟢 [意图识别节点] 接收到用户输入: {} (Session: {})", userQuery, threadId);

            // 意图分类
            String classifyPrompt = String.format(
                    "请判断以下用户输入是'闲聊'还是'查询天气'。只返回 'chat' 或 'weather'，不要返回其他内容。\n用户输入: %s",
                    userQuery
            );

            String intent = dashscopeChatClient.prompt()
                    .user(classifyPrompt)
                    .call()
                    .content()
                    .trim()
                    .toLowerCase();

            log.info("🟢 [意图识别节点] 识别结果: {}", intent);

            // 如果是闲聊，使用 ChatMemory 进行多轮对话
            if ("chat".equals(intent)) {
                String chatReply = dashscopeChatClient.prompt()
                        .user(userQuery)
                        .advisors(a -> a.param("chat_memory_conversation_id", threadId))
                        .call()
                        .content();
                log.info("🟢 [意图识别节点] 闲聊回复: {}", chatReply);
                return Map.of("intent", "chat", "chat_response", chatReply);
            }

            return Map.of("intent", "weather");
        }));

        // 4. 节点2：天气查询 Agent（自带 RedisSaver 记忆）
        stateGraph.addNode("weather_agent_node", reactAgent.asNode(true, false));

        // 5. 日志节点
        stateGraph.addNode("log_agent_result", node_async(state -> {
            AssistantMessage message = (AssistantMessage) state.value("agent_response").orElse(null);
            String agentResponse = (message != null) ? message.getText() : "无响应";
            log.info("🤖 [Agent节点] 执行完毕，最终回复: {}", agentResponse);
            return Map.of();
        }));

        // 6. 添加边
        stateGraph.addEdge(StateGraph.START, "intent_node");

        // 7. 条件边（动态路由）
        stateGraph.addConditionalEdges(
                "intent_node",
                edge_async(state -> {
                    String intent = (String) state.value("intent").orElse("");
                    log.info("🔀 [路由节点] 当前意图: {}，准备路由...", intent);
                    return intent;
                }),
                Map.of(
                        "chat", StateGraph.END,
                        "weather", "weather_agent_node"
                )
        );

        stateGraph.addEdge("weather_agent_node", "log_agent_result");
        stateGraph.addEdge("log_agent_result", StateGraph.END);

        log.info("✅ [Graph] 天气工作流编译完成！");
        return stateGraph.compile();
    }
}