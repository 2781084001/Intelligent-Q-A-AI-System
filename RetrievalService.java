package com.shengfangzhi.qa.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 知识检索服务（简化版，实际应接入向量数据库）
 */
@Slf4j
@Service
public class RetrievalService {

    @Value("${qa.retrieval-top-k:3}")
    private int topK;

    // 模拟知识库（实际应从向量数据库检索）
    private final Map<String, List<String>> knowledgeBase = initKnowledgeBase();

    /**
     * 语义检索相关文档
     */
    public String retrieve(String question) {
        log.debug("检索问题: {}", question);

        // 提取关键词（简化版，实际应使用 Embedding）
        List<String> keywords = extractKeywords(question);

        // 匹配相关文档
        List<ScoredDoc> scoredDocs = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : knowledgeBase.entrySet()) {
            String category = entry.getKey();
            List<String> docs = entry.getValue();
            for (String doc : docs) {
                int score = calculateRelevance(question, keywords, doc);
                if (score > 0) {
                    scoredDocs.add(new ScoredDoc(doc, score));
                }
            }
        }

        // 排序并取 Top-K
        scoredDocs.sort((a, b) -> Integer.compare(b.score, a.score));
        List<String> topDocs = scoredDocs.stream()
            .limit(topK)
            .map(d -> d.doc)
            .toList();

        log.info("检索到 {} 条相关文档，top1 分数: {}", 
                 topDocs.size(), 
                 scoredDocs.isEmpty() ? 0 : scoredDocs.get(0).score);

        if (topDocs.isEmpty()) {
            return "未找到相关技术文档。";
        }

        return String.join("\n\n---\n\n", topDocs);
    }

    /**
     * 获取原始文档（降级使用）
     */
    public String retrieveRaw(String question) {
        String retrieved = retrieve(question);
        if ("未找到相关技术文档。".equals(retrieved)) {
            return "抱歉，未找到与您问题相关的技术文档。建议您：\n1. 尝试使用更具体的关键词\n2. 联系技术支持团队";
        }
        return retrieved;
    }

    /**
     * 提取关键词（简化版）
     */
    private List<String> extractKeywords(String question) {
        // 实际应使用分词工具如 HanLP
        String[] words = question.toLowerCase()
            .replaceAll("[，,。？?！!；;]", " ")
            .split("\\s+");
        return Arrays.stream(words)
            .filter(w -> w.length() > 1)
            .limit(5)
            .toList();
    }

    /**
     * 计算相关性分数
     */
    private int calculateRelevance(String question, List<String> keywords, String doc) {
        String lowerDoc = doc.toLowerCase();
        int score = 0;
        for (String keyword : keywords) {
            if (lowerDoc.contains(keyword)) {
                score++;
            }
        }
        // 完整问题匹配加分
        if (lowerDoc.contains(question.toLowerCase())) {
            score += 5;
        }
        return score;
    }

    /**
     * 初始化模拟知识库
     */
    private Map<String, List<String>> initKnowledgeBase() {
        Map<String, List<String>> kb = new HashMap<>();

        kb.put("Spring Boot", List.of(
            "Spring Boot 配置多数据源：在 application.yml 中配置多个 DataSource，使用 @Primary 注解指定主数据源...",
            "Spring Boot 整合 MyBatis：添加 mybatis-spring-boot-starter 依赖，配置 mapper 扫描路径...",
            "Spring Boot 健康检查：引入 spring-boot-starter-actuator，访问 /actuator/health 端点..."
        ));

        kb.put("Docker", List.of(
            "Docker 常用命令：docker run、docker ps、docker exec、docker logs、docker-compose up...",
            "Dockerfile 编写：FROM 指定基础镜像，COPY 复制文件，RUN 执行命令，CMD 指定启动命令..."
        ));

        kb.put("Linux", List.of(
            "Linux 查看日志：tail -f /var/log/app.log，grep 'ERROR' app.log...",
            "Linux 进程管理：ps aux | grep java，kill -9 PID，nohup java -jar app.jar &..."
        ));

        kb.put("Redis", List.of(
            "Redis 分布式锁：使用 SETNX 命令，配合过期时间防止死锁，Redisson 提供封装实现...",
            "Redis 缓存穿透解决：布隆过滤器、缓存空对象、热点数据预热..."
        ));

        return kb;
    }

    @lombok.AllArgsConstructor
    private static class ScoredDoc {
        String doc;
        int score;
    }
}
