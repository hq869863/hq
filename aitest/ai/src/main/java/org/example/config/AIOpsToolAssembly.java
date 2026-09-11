package org.example.config;

import org.example.tool.SystemInfoTools;
import org.example.tool.SystemTools;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author hq
 */
@Configuration
public class AIOpsToolAssembly {

    private final List<ToolCallbackProvider> toolProviders = new ArrayList<>();

    // 1. 构造器中注入并注册所有本地 Tool
    public AIOpsToolAssembly(SystemTools systemTools,
                             SystemInfoTools systemInfoTools
//            , ToolCallbackProvider toolCallbackProvider
    ) {
        ToolCallbackProvider localProvider = MethodToolCallbackProvider.builder()
                .toolObjects(systemTools, systemInfoTools)
                .build();
        this.toolProviders.add(localProvider);
//        this.toolProviders.add(toolCallbackProvider);
    }

    // 4. 获取最终合并后的所有工具（本地 + 所有 MCP）
    public ToolCallback[] getAllTools() {
        return toolProviders.stream()
                .flatMap(provider -> Arrays.stream(provider.getToolCallbacks()))
                .toArray(ToolCallback[]::new);
    }
}
