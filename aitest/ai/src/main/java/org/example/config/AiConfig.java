package org.example.config;

import com.alibaba.cloud.ai.memory.redis.RedisChatMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;

/**
 * @author hq
 */
@Slf4j
@Configuration
public class AiConfig {

//    @Bean
//    public JedisPooled jedisPooled() {
//        JedisClientConfig config = DefaultJedisClientConfig.builder()
//                .password("123456")
//                .build();
//        return new JedisPooled("localhost", 6379);
//    }
//
//    @Bean
//    public VectorStore vectorStore(JedisPooled jedisPooled, EmbeddingModel embeddingModel) {
//        return RedisVectorStore.builder(jedisPooled, embeddingModel)
//                .indexName("aitest-index")
//                .build();
//    }

    // ==================== 1. 模型配置 ====================

    /**
     * 解决多模型冲突
     */
    @Bean
    @Primary
    public ChatModel primaryChatModel(@Qualifier("dashScopeChatModel") ChatModel dashscopeChatModel) {
        return dashscopeChatModel;
    }


    // ==================== 2. 记忆存储配置 ====================

    /**
     * 基于 Redis 的聊天记忆仓库
     */
    @Bean
    public RedisChatMemoryRepository chatMemoryRepository(
            @Value("${spring.ai.memory.redis.host}") String host,
            @Value("${spring.ai.memory.redis.port}") int port,
            @Value("${spring.ai.memory.redis.password}") String password) {

        return RedisChatMemoryRepository.builder()
                .host(host)
                .port(port)
                .password(password)
                .build();
    }

    // ==================== 3. 记忆策略配置 ====================

    /**
     * 记忆策略
     */
    @Bean
    public ChatMemory chatMemory(RedisChatMemoryRepository redisChatMemoryRepository,
                                 @Value("${app.ai.max-messages}") int maxMessages) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(maxMessages)
                .build();
    }

    // ==================== 4. Advisor（拦截器）配置 ====================

    /**
     * 记忆顾问：在请求发给大模型之前，先经过一层“拦截加工”
     */
    @Bean
    public MessageChatMemoryAdvisor memoryChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    /**
     * RAG 检索顾问：在请求发给大模型之前，先经过一层“拦截加工”
     */
    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().build())
                .build();
    }

    // ==================== 5. ChatClient 组装 ====================

    /**
     * 构建 DashScope 的 ChatClient
     */
    @Bean
    @Primary
    public ChatClient dashscopeChatClient(@Qualifier("dashScopeChatModel") ChatModel dashscopeChatModel,
                                          MessageChatMemoryAdvisor memoryAdvisor,
                                          AIOpsToolAssembly toolAssembly,
                                          QuestionAnswerAdvisor ragAdvisor,
                                          @Value("${app.ai.default-system-prompt}") String systemPrompt) {

        // 一键获取所有工具（本地 Tool + K8s/Prometheus/GitHub MCP 工具）
        ToolCallback[] allTools = toolAssembly.getAllTools();
        System.out.println("已加载的工具列表: " + Arrays.toString(allTools));
        return ChatClient.builder(dashscopeChatModel)
                .defaultSystem(systemPrompt)
                .defaultToolCallbacks(allTools)
                .defaultAdvisors(memoryAdvisor, ragAdvisor, new SimpleLoggerAdvisor())
                .build();
    }

    /**
     * 构建 Ollama 的 ChatClient
     */
//    @Bean
//    public ChatClient ollamaChatClient(@Qualifier("ollamaChatModel") ChatModel ollamaChatModel,
//                                       MessageChatMemoryAdvisor memoryAdvisor,
//                                       QuestionAnswerAdvisor ragAdvisor,
//                                       @Value("${app.ai.default-system-prompt}") String systemPrompt) {
//        return ChatClient.builder(ollamaChatModel)
//                .defaultSystem(systemPrompt)
//                .defaultAdvisors(memoryAdvisor, ragAdvisor, new SimpleLoggerAdvisor())
//                .build();
//    }
}