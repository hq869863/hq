package org.example.config;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author hq
 */
@Configuration
public class McpClientConfig {

    @Value("${baidu.map.ak}")
    private String baiduMapAk;

    /**
     * 创建并配置连接到百度地图 MCP 服务的同步客户端。
     *
     * @return 配置好的 McpSyncClient Bean
     */
    @Bean
    public McpSyncClient baiduMapMcpSyncClient() {
        // 1. 构建远程服务的完整 URL
        String serverUrl = "https://mcp.map.baidu.com/sse?ak=" + baiduMapAk;

        // 2. 创建一个 WebClient，专门用于与 MCP 服务通信
        WebClient webClient = WebClient.builder()
                .baseUrl(serverUrl)
                .build();

        // 3. 使用 SseClientTransport 作为传输层，连接到远程 HTTP 服务
        SseClientTransport transport = new SseClientTransport(webClient);

        // 4. 使用 McpClientProvider 来创建并初始化同步客户端
        //    这里设置了连接超时时间，你可以根据网络情况调整
        return McpClientProvider.sync(transport)
                .initializationTimeout(Duration.ofSeconds(10))
                .build();
    }
}