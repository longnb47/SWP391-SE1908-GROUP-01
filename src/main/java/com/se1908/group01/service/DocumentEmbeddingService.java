package com.se1908.group01.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DocumentEmbeddingService {

	private final EmbeddingModel embeddingModel;
	private final ObjectMapper objectMapper;

	public DocumentEmbeddingService(@Nullable EmbeddingModel embeddingModel, ObjectMapper objectMapper) {
		this.embeddingModel = embeddingModel;
		this.objectMapper = objectMapper;
	}

	public List<String> embedVectors(List<String> texts) {
		if (texts == null || texts.isEmpty()) {
			return List.of();
		}

		if (embeddingModel == null) {
			throw new IllegalStateException("EmbeddingModel is not configured. Set SPRING_AI_MODEL_EMBEDDING_TEXT=google-genai and GEMINI_API_KEY.");
		}

		var cleaned = new ArrayList<String>(texts.size());
		for (String t : texts) {
			cleaned.add(StringUtils.hasText(t) ? t : "");
		}

		var response = embeddingModel.embedForResponse(cleaned);
		var results = response.getResults();
		if (results == null || results.size() != cleaned.size()) {
			throw new IllegalStateException("Embedding response size mismatch");
		}

		List<String> vectors = new ArrayList<>(cleaned.size());
		for (var r : results) {
			var output = r.getOutput();
			try {
				vectors.add(objectMapper.writeValueAsString(output));
			} catch (JsonProcessingException e) {
				throw new IllegalStateException("Failed to serialize embedding vector", e);
			}
		}
		return vectors;
	}
}
