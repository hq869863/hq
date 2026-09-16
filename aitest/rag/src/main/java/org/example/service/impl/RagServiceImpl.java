package org.example.service.impl;

import org.example.service.RagService;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author hq
 */
@Service
public class RagServiceImpl implements RagService {


    private final VectorStore vectorStore;

    public RagServiceImpl(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 【方法一：文档读取、切分与入库】
     * 作用：将非结构化文件转化为向量并存入 Redis
     */
    @Override
    public void loadDocument() {
        // 1. 使用 Tika 读取本地文件（支持 txt, pdf, docx 等）
        TikaDocumentReader reader = new TikaDocumentReader(new ClassPathResource("rag.txt"));
        List<Document> documents = reader.get();

        // 2. 为文档添加自定义元数据 (Metadata)
        // 业务意义：后续检索时，可以通过 metadata 进行精确过滤（例如：只搜某个部门的文档）
        documents.forEach(doc -> {
            doc.getMetadata().put("source", "local-file");
            doc.getMetadata().put("department", "tech");
        });

        // 3. 文本切分 (Chunking)
        // 参数说明：(defaultChunkSize=800, minChunkSizeChars=100, minChunkLengthToEmbed=5,
        //           maxNumChunks=10000, keepSeparator=true)
        // 业务意义：防止单次输入超出大模型上下文窗口，同时保留合理的语义完整性
        TokenTextSplitter splitter = new TokenTextSplitter(800, 100, 5, 10000, true);
        List<Document> chunks = splitter.apply(documents);

        // 4. 批量向量化并写入 Redis
        vectorStore.add(chunks);
        System.out.println("成功入库 " + chunks.size() + " 个文本块！");
    }

    /**
     * 【方法二：基础向量检索】
     * 作用：根据用户问题，从向量库中找出最相关的文本块
     */
    @Override
    public List<Document> basicSearch(String userQuery) {
        // 构建检索请求
        SearchRequest request = SearchRequest.builder()
                .query(userQuery)
                .topK(3)
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * 【方法三：带元数据过滤的检索】
     * 作用：在语义相似的基础上，强制限定检索范围（多租户/权限隔离必备）
     */
    @Override
    public List<Document> filterSearch(String userQuery, String department) {
        SearchRequest request = SearchRequest.builder()
                .query(userQuery)
                .topK(3)
                // 核心：通过 filterExpression 进行元数据精确匹配
                // 语法类似 SQL 的 WHERE 条件，这里表示 department 字段必须等于传入的值
                .filterExpression("department == '" + department + "'")
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * 【方法四：带相似度阈值与 MMR 策略的检索（进阶推荐）】
     * 作用：防止大模型“胡说八道”，同时保证检索结果的多样性
     */
    @Override
    public List<Document> advancedSearch(String userQuery) {
        SearchRequest request = SearchRequest.builder()
                .query(userQuery)
                .topK(5)
                // 核心1：相似度阈值 (0.0 ~ 1.0)
                // 业务意义：低于 0.7 的结果直接丢弃，避免把不相关的资料喂给大模型导致幻觉
                .similarityThreshold(0.7)
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * 【方法五：删除指定文档】
     * 作用：当本地文件更新或失效时，清理向量库中的旧数据
     */
    @Override
    public void deleteDocuments(List<String> documentIds) {
        // 传入文档 ID 列表，从 Redis 中彻底删除对应的向量数据
        vectorStore.delete(documentIds);
        System.out.println("成功删除 " + documentIds.size() + " 个文本块！");
    }
}