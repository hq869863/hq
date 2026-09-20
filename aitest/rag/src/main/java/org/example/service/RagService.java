package org.example.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * @author hq
 */
public interface  RagService {

    /** 读取、切分并入库文档 */
    void loadDocument();

    /** 基础向量检索 */
    List<Document> basicSearch(String userQuery);

    /** 带元数据过滤的检索 */
    List<Document> filterSearch(String userQuery, String department);

    /** 带相似度阈值的高级检索 */
    List<Document> advancedSearch(String userQuery);

    /** 删除指定文档 */
    void deleteDocuments(List<String> documentIds);
}
