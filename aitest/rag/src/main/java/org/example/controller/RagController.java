package org.example.controller;

import org.example.service.RagService;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author hq
 */
@RestController
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * 1. 导入并切分文档
     */
    @GetMapping("/load")
    public String loadDoc() {
        ragService.loadDocument();
        return "文档解析并向量化完成！";
    }

    /**
     * 2. 基础向量检索（仅测试检索效果，不调用大模型）
     * 测试示例: GET /api/rag/search/basic?query=Redis怎么配置密码
     */
    @GetMapping("/search/basic")
    public List<Map<String, Object>> basicSearch(@RequestParam String query) {
        List<Document> docs = ragService.basicSearch(query);
        return docs.stream().map(doc -> Map.of(
                "text", doc.getText(),
                "metadata", doc.getMetadata()
        )).collect(Collectors.toList());
    }

    /**
     * 3. 带元数据过滤的检索（测试权限隔离/范围限定）
     * 测试示例: GET /api/rag/search/filter?query=Redis配置&department=tech
     */
    @GetMapping("/search/filter")
    public List<Map<String, Object>> filterSearch(
            @RequestParam String query,
            @RequestParam String department) {
        List<Document> docs = ragService.filterSearch(query, department);
        return docs.stream().map(doc -> Map.of(
                "text", doc.getText(),
                "metadata", doc.getMetadata()
        )).collect(Collectors.toList());
    }

    /**
     * 4. 高级检索（带相似度阈值，过滤低质量结果）
     * 测试示例: GET /api/rag/search/advanced?query=Redis怎么配置密码
     */
    @GetMapping("/search/advanced")
    public List<Map<String, Object>> advancedSearch(@RequestParam String query) {
        List<Document> docs = ragService.advancedSearch(query);
        return docs.stream().map(doc -> Map.of(
                "text", doc.getText(),
                "metadata", doc.getMetadata()
        )).collect(Collectors.toList());
    }

    /**
     * 5. 删除指定文档（根据文档ID清理向量库）
     * 测试示例: DELETE /api/rag/delete?ids=id1,id2,id3
     */
    @DeleteMapping("/delete")
    public String deleteDocs(@RequestParam List<String> ids) {
        ragService.deleteDocuments(ids);
        return "成功删除 " + ids.size() + " 个文本块！";
    }
}