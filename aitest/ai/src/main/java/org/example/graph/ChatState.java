package org.example.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hq
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatState implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户当前输入
     */
    private String input;

    /**
     * 意图识别结果
     */
    private String intent;

    /**
     * 最终输出，返回给前端的内容
     */
    private String output;

    /**
     * 当前人设，例如 "你是一个专业的翻译助手"
     * 跨轮次持久化，用户切换人设后一直生效
     */
    private String persona;

    /**
     * 安全检查结果，true 表示通过
     */
    private Boolean safe;

}