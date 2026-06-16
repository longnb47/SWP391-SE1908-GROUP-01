package com.se1908.group01.service.impl;

import com.se1908.group01.dto.RetrievedChunk;
import com.se1908.group01.service.PromptBuilderService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PromptBuilderServiceImpl implements PromptBuilderService {

	private static final int MAX_CHUNK_CHARACTERS = 2000;

	@Override
	public String buildDocumentQuestionPrompt(String question, List<RetrievedChunk> chunks) {
		var prompt = new StringBuilder();
		prompt.append("""
				You are an AI study assistant.
				Answer the user's question using only the provided document context.
				If the answer is not present in the context, say: "The answer cannot be found in the selected document."
				Do not use outside knowledge.
				Do not invent citations, facts, or document content.

				Document context:
				""");

		for (int i = 0; i < chunks.size(); i++) {
			var retrieved = chunks.get(i);
			var chunk = retrieved.getChunk();
			prompt.append("\n[Chunk ")
					.append(i + 1)
					.append(" | chunkIndex=")
					.append(chunk.getChunkIndex());
			if (chunk.getPageNumber() != null) {
				prompt.append(" | page=").append(chunk.getPageNumber());
			}
			prompt.append("]\n")
					.append(truncate(chunk.getContent()))
					.append("\n");
		}

		prompt.append("\nUser question:\n")
				.append(question);

		return prompt.toString();
	}

	private String truncate(String content) {
		if (content == null || content.length() <= MAX_CHUNK_CHARACTERS) {
			return content;
		}
		return content.substring(0, MAX_CHUNK_CHARACTERS);
	}
}
