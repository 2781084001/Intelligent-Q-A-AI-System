package com.shengfangzhi.qa.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.shengfangzhi.qa.dto.QaRequest;
import com.shengfangzhi.qa.dto.QaResponse;
import com.shengfangzhi.qa.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class QaService {

    private final RetrievalService retrievalService;
    private final ModelInferenceService modelService;
    private final CacheUtil cacheUtil;

    @Value("${qa.max-history-rounds:5}")
    private int maxHistoryRounds;

    // 存储会话历史（生产环境应使用 Redis）
    private final ConcurrentHashMap<String, ConversationHistory> sessionHistory = new ConcurrentHashMap<>();

    /**
     * 普通问答（非流式）
     */
    public QaResponse ask(QaRequest request) {
        String sessionId = request.getSessionId();
        String question = request.getQuestion();

        // 1. 检查缓存
        String cacheKey = CacheUtil.buildCacheKey(sessionId, question);
        String cachedAnswer = cacheUtil.get(cacheKey);
        if (cachedAnswer != null) {
            log.info("命中缓存，sessionId: {}", sessionId);
            return QaResponse.success(cachedAnswer, true);
        }

        long startTime = System.currentTimeMillis();

        try {
            // 2. 获取对话历史
            String historyContext = getHistoryContext(sessionId);

            // 3. 检索相关知识
            String retrievedContext = retrievalService.retrieve(question);

            // 4. 构建 Prompt
            String prompt = buildPrompt(question, historyContext, retrievedContext);

            // 5. 调用模型生成答案
            String answer = modelService.generate(prompt);

            // 6. 保存历史
            saveHistory(sessionId, question, answer);

            // 7. 写入缓存
            cacheUtil.put(cacheKey, answer);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("问答完成，sessionId: {}, 耗时: {}ms", sessionId, elapsed);

            // 8. 异步发送日志到 RocketMQ
            sendLogToMQ(sessionId, question, answer, elapsed);

            return QaResponse.success(answer, false);

        } catch (Exception e) {
            log.error("问答失败，sessionId: {}", sessionId, e);
            // 降级：返回检索到的原始文档
            String fallbackAnswer = retrievalService.retrieveRaw(question);
            return QaResponse.error(fallbackAnswer, e.getMessage());
        }
    }

    /**
     * 流式问答
     */
    public Flux<String> askStream(QaRequest request) {
        String sessionId = request.getSessionId();
        String question = request.getQuestion();

        return modelService.generateStream(question)
                .doOnComplete(() -> log.info("流式问答完成，sessionId: {}", sessionId))
                .doOnError(e -> log.error("流式问答失败，sessionId: {}", sessionId, e));
    }

    /**
     * 构建 Prompt
     */
    private String buildPrompt(String question, String historyContext, String retrievedContext) {
        return """
            你是一个专业的技术支持助手，请基于以下上下文回答用户的问题。

            ## 对话历史
            %s

            ## 知识库内容
            %s

            ## 用户问题
            %s

            请给出准确、简洁、有帮助的回答。如果无法从上下文中找到答案，请如实告知。
            """.formatted(historyContext, retrievedContext, question);
    }

    /**
     * 获取对话历史
     */
    private String getHistoryContext(String sessionId) {
        ConversationHistory history = sessionHistory.get(sessionId);
        if (history == null || history.getMessages().isEmpty()) {
            return "无历史对话";
        }
        return history.getRecentMessages(maxHistoryRounds);
    }

    /**
     * 保存对话历史
     */
    private void saveHistory(String sessionId, String question, String answer) {
        ConversationHistory history = sessionHistory.computeIfAbsent(
            sessionId, k -> new ConversationHistory()
        );
        history.addMessage(question, answer);
    }

    /**
     * 异步发送日志到 RocketMQ
     */
    private void sendLogToMQ(String sessionId, String question, String answer, long elapsed) {
        // 实际实现中使用 RocketMQ 模板发送
        log.debug("发送问答日志到 MQ，sessionId: {}, 耗时: {}ms", sessionId, elapsed);
    }

    /**
     * 内部类：对话历史管理
     */
    private static class ConversationHistory {
        private final List<Message> messages = new ArrayList<>();

        public void addMessage(String question, String answer) {
            messages.add(new Message(question, answer, LocalDateTime.now()));
        }

        public String getRecentMessages(int rounds) {
            return messages.stream()
                .skip(Math.max(0, messages.size() - rounds))
                .map(m -> "用户: " + m.question + "\n助手: " + m.answer)
                .collect(Collectors.joining("\n\n"));
        }

        public List<Message> getMessages() {
            return messages;
        }

        @lombok.Value
        private static class Message {
            String question;
            String answer;
            LocalDateTime timestamp;
        }
    }
}
