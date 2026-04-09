package com.shengfangzhi.qa.controller;

import com.shengfangzhi.qa.dto.QaRequest;
import com.shengfangzhi.qa.dto.QaResponse;
import com.shengfangzhi.qa.service.QaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
public class QaController {

    private final QaService qaService;

    /**
     * 普通问答接口
     */
    @PostMapping("/ask")
    public QaResponse ask(@RequestBody QaRequest request) {
        log.info("收到问答请求，sessionId: {}, question: {}", 
                 request.getSessionId(), request.getQuestion());
        return qaService.ask(request);
    }

    /**
     * 流式问答接口（SSE）
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askStream(@RequestBody QaRequest request) {
        log.info("收到流式问答请求，sessionId: {}, question: {}", 
                 request.getSessionId(), request.getQuestion());
        return qaService.askStream(request);
    }
}
