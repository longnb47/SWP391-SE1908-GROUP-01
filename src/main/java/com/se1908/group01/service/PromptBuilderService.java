package com.se1908.group01.service;

import com.se1908.group01.dto.RetrievedChunk;
import com.se1908.group01.enums.ChatMode;
import com.se1908.group01.enums.KnowledgePolicy;
import java.util.List;

public interface PromptBuilderService {

    String buildDocumentQuestionPrompt(String question, List<RetrievedChunk> chunks);

    String buildMultiDocumentQuestionPrompt(ChatMode mode, KnowledgePolicy knowledgePolicy, String context, String question);
}
