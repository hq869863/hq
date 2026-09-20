package org.example.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
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

        RunnableConfig config = RunnableConfig.builder()
                .threadId(chatId)
                .build();
        var result = weatherGraph.invoke(Map.of("input", userInput), config);
        return result
                .map(state -> state.value("output", String.class).orElse("抱歉，服务暂时不可用"))
                .orElse("抱歉，服务暂时不可用");
    }
}
