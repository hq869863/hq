package org.example.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author hq
 */
@Component
public class SystemTools {

    /**
     * 1. 数据预处理与格式化（“翻译官”）
     * 将复杂的原始监控数据转换为大模型易读的简洁文本
     */
    @Tool(description = "将复杂的原始监控 JSON 数据转换为简洁的文本摘要，节省 Token 并提高可读性。")
    public String formatMetrics(String rawJsonData) {
        // 实际场景中，这里会使用 Jackson/Gson 解析 JSON 并提取关键字段
        // 这里用模拟逻辑演示
        if (rawJsonData.contains("cpu")) {
            return "【指标摘要】过去 5 分钟内，订单服务 CPU 平均使用率为 85%，峰值达到 92%。";
        }
        return "【指标摘要】未识别到关键异常指标。";
    }

    /**
     * 2. 变更影响分析（“预言家”）
     * 基于本地服务拓扑，评估操作的影响范围
     */
    @Tool(description = "分析重启或修改某个服务的影响范围。输入服务名，输出受影响的下游业务列表。")
    public String analyzeImpact(String serviceName) {
        // 模拟本地的服务依赖拓扑数据
        Map<String, List<String>> topology = new HashMap<>();
        topology.put("order-service", Arrays.asList("payment-gateway", "inventory-service", "notification-service"));
        topology.put("user-service", Arrays.asList("auth-service", "profile-service"));

        List<String> affected = topology.getOrDefault(serviceName, Collections.emptyList());
        if (affected.isEmpty()) {
            return "服务 " + serviceName + " 无下游依赖，变更风险极低。";
        }
        return "警告：重启 " + serviceName + " 将直接影响以下核心业务：" + String.join(", ", affected) + "。请谨慎操作！";
    }

    /**
     * 3. 结果校验与自检（“质检员”）
     * 对 Agent 生成的脚本进行安全兜底
     */
    @Tool(description = "校验生成的 Shell 脚本或 SQL 语句是否包含高危命令。输入待校验的脚本内容。")
    public String validateScriptSafety(String scriptContent) {
        List<String> dangerousKeywords = Arrays.asList("rm -rf", "drop table", "delete from", "format");
        String lowerScript = scriptContent.toLowerCase();

        for (String keyword : dangerousKeywords) {
            if (lowerScript.contains(keyword)) {
                return "【安全拦截】检测到高危命令：" + keyword + "。禁止执行！";
            }
        }
        return "【安全通过】脚本内容安全，允许执行。";
    }

    /**
     * 4. 知识库检索（RAG 的“检索器”）
     * 从本地向量库中检索历史故障案例
     */
    @Tool(description = "根据故障描述，从本地运维知识库中检索最相似的历史故障案例和处理方案。")
    public String searchKnowledgeBase(String faultDescription) {
        // 实际场景中，这里会调用 Milvus/Elasticsearch 进行向量检索
        // 这里用模拟逻辑演示
        if (faultDescription.contains("OOM") || faultDescription.contains("内存")) {
            return "【历史案例匹配】找到相似案例：2023-10-01 订单服务 OOM。\n" +
                    "【根因】新版本代码存在缓存未释放问题。\n" +
                    "【解决方案】立即回滚至 v1.2 版本，并重启 Pod。";
        }
        return "【检索结果】未找到高度匹配的历史故障案例。";
    }

    @Tool(description = "获取当前服务器的准确系统时间、日期和时区。当用户询问现在几点、计算时间差或处理时间敏感任务时使用。")
    public String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}