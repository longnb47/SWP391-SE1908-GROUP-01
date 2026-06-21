package com.se1908.group01.service;

import com.se1908.group01.dto.RetrievedChunk;
import java.util.List;

public interface PromptBuilderService {

	String buildDocumentQuestionPrompt(String question, List<RetrievedChunk> chunks);
}
