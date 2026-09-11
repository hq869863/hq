package org.example.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author hq
 */
@RestController
public class GarphController {
    private static final Logger log = LoggerFactory.getLogger(GarphController.class);
    private final CompiledGraph weatherGraph;

    public GarphController(CompiledGraph weatherGraph) {
        this.weatherGraph = weatherGraph;
    }

    @PostMapping("/graph/{chatId}")
    public String runGraph(@RequestBody String userInput, @PathVariable("chatId") String chatId) throws Exception {
        // 传入初始状态
        Map<String, Object> inputs = Map.of("user_input", userInput,
                                            "thread_id", chatId);

        // 执行 Graph
        var result = weatherGraph.invoke(inputs);

        // 获取最终状态中的结果（根据实际 State 结构调整 key）
        return result.toString();
    }
}
