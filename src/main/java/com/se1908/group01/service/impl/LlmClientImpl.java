package com.se1908.group01.service.impl;

import com.se1908.group01.dto.AiGenerationOptions;
import com.se1908.group01.service.AiChatClientService;
import com.se1908.group01.service.LlmClient;
import org.springframework.stereotype.Service;

@Service
public class LlmClientImpl implements LlmClient {

    private final AiChatClientService aiChatClientService;

    public LlmClientImpl(AiChatClientService aiChatClientService) {
        this.aiChatClientService = aiChatClientService;
    }

    @Override
    public String generateAnswer(String prompt, AiGenerationOptions options) {
        return aiChatClientService.ask(prompt, options);
    }
}
