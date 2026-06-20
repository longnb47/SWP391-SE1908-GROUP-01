package com.se1908.group01.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DocumentEmbeddingService {

	private static final Logger log = LoggerFactory.getLogger(DocumentEmbeddingService.class);
	private static final int MAX_EMBEDDING_BATCH_SIZE = 90;
	private static final int MAX_RETRY_ATTEMPTS = 3;
	private static final Duration DEFAULT_RETRY_DELAY = Duration.ofSeconds(30);
	private static final Duration MAX_RETRY_DELAY = Duration.ofSeconds(90);
	private static final Pattern RETRY_INFO_PATTERN = Pattern.compile("retryDelay\"\\s*:\\s*\"(\\d+)s\"");
	private static final Pattern PLEASE_RETRY_PATTERN = Pattern.compile("Please retry in\\s+([0-9]+(?:\\.[0-9]+)?)s");

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

		List<String> vectors = new ArrayList<>(cleaned.size());
		for (int start = 0; start < cleaned.size(); start += MAX_EMBEDDING_BATCH_SIZE) {
			var end = Math.min(start + MAX_EMBEDDING_BATCH_SIZE, cleaned.size());
			var batch = cleaned.subList(start, end);
			var response = embedBatchWithRetry(batch);
			var results = response.getResults();
			if (results == null || results.size() != batch.size()) {
				throw new IllegalStateException("Embedding response size mismatch");
			}
			for (var r : results) {
				var output = r.getOutput();
				try {
					vectors.add(objectMapper.writeValueAsString(output));
				} catch (JsonProcessingException e) {
					throw new IllegalStateException("Failed to serialize embedding vector", e);
				}
			}
		}
		return vectors;
	}

	private EmbeddingResponse embedBatchWithRetry(List<String> batch) {
		for (int attempt = 0; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
			try {
				return embeddingModel.embedForResponse(batch);
			} catch (RuntimeException ex) {
				if (!isQuotaError(ex) || attempt >= MAX_RETRY_ATTEMPTS) {
					throw ex;
				}
				var delay = extractRetryDelay(ex);
				log.warn(
						"Gemini embedding quota reached. Retrying batch in {} seconds. attempt={}/{} batchSize={}",
						delay.toSeconds(),
						attempt + 1,
						MAX_RETRY_ATTEMPTS,
						batch.size()
				);
				sleep(delay);
			}
		}
		throw new IllegalStateException("Embedding retry attempts exhausted");
	}

	private boolean isQuotaError(RuntimeException ex) {
		var message = ex.getMessage();
		if (!StringUtils.hasText(message)) {
			return false;
		}
		return message.contains("429")
				|| message.contains("Quota exceeded")
				|| message.contains("retryDelay")
				|| message.contains("Please retry in");
	}

	private Duration extractRetryDelay(RuntimeException ex) {
		var message = ex.getMessage();
		if (!StringUtils.hasText(message)) {
			return DEFAULT_RETRY_DELAY;
		}

		var retryInfoMatcher = RETRY_INFO_PATTERN.matcher(message);
		if (retryInfoMatcher.find()) {
			return clampRetryDelay(Duration.ofSeconds(Long.parseLong(retryInfoMatcher.group(1)) + 1));
		}

		var pleaseRetryMatcher = PLEASE_RETRY_PATTERN.matcher(message);
		if (pleaseRetryMatcher.find()) {
			var seconds = Math.ceil(Double.parseDouble(pleaseRetryMatcher.group(1)));
			return clampRetryDelay(Duration.ofSeconds((long) seconds + 1));
		}

		return DEFAULT_RETRY_DELAY;
	}

	private Duration clampRetryDelay(Duration delay) {
		if (delay.isNegative() || delay.isZero()) {
			return DEFAULT_RETRY_DELAY;
		}
		if (delay.compareTo(MAX_RETRY_DELAY) > 0) {
			return MAX_RETRY_DELAY;
		}
		return delay;
	}

	private void sleep(Duration delay) {
		try {
			Thread.sleep(delay.toMillis());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Embedding retry was interrupted", ex);
		}
	}
}
