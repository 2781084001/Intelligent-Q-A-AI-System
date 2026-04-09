package com.shengfangzhi.qa.service;

import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

@Slf4j
@Service
public class ModelInferenceService {

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model.code:deepseek-coder:6.7b}")
    private String codeModelName;

    @Value("${ollama.model.general:qwen:7b}")
    private String generalModelName;

    @Value("${qa.timeout-seconds:15}")
    private int timeoutSeconds;

    private OllamaChatModel chatModel;

    @PostConstruct
    public void init() {
        // 初始化 Ollama 客户端
        chatModel = OllamaChatModel.builder()
            .baseUrl(ollamaBaseUrl)
            .modelName(generalModelName)
            .temperature(0.7)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .build();
        log.info("模型推理服务初始化完成，使用模型: {}", generalModelName);
    }

    /**
     * 生成答案
     */
    public String generate(String prompt) {
        try {
            log.debug("调用模型生成答案，prompt 长度: {}", prompt.length());
            String answer = chatModel.generate(prompt);
            log.debug("模型返回答案，长度: {}", answer.length());
            return answer;
        } catch (Exception e) {
            log.error("模型调用失败", e);
            throw new RuntimeException("模型推理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式生成答案
     */
    public Flux<String> generateStream(String prompt) {
        log.debug("开始流式生成，prompt 长度: {}", prompt.length());
        return Flux.fromIterable(() -> chatModel.generate(prompt).lines().iterator())
            .delayElements(Duration.ofMillis(50));
    }
}
