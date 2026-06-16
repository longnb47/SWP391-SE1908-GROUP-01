package com.se1908.group01.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se1908.group01.dto.RetrievedChunk;
import com.se1908.group01.entity.DocumentChunk;
import com.se1908.group01.repository.DocumentChunkRepository;
import com.se1908.group01.service.VectorSearchService;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VectorSearchServiceImpl implements VectorSearchService {

	private final DocumentChunkRepository documentChunkRepository;
	private final ObjectMapper objectMapper;

	public VectorSearchServiceImpl(DocumentChunkRepository documentChunkRepository, ObjectMapper objectMapper) {
		this.documentChunkRepository = documentChunkRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public List<RetrievedChunk> search(Long documentId, String queryEmbeddingVector, int limit) {
		if (documentId == null) {
			throw new IllegalArgumentException("Document ID is required");
		}
		if (!StringUtils.hasText(queryEmbeddingVector)) {
			throw new IllegalArgumentException("Query embedding vector is required");
		}

		var queryVector = parseVector(queryEmbeddingVector);
		return documentChunkRepository.findByDocumentDocumentIdOrderByChunkIndexAsc(documentId)
				.stream()
				.map(chunk -> score(chunk, queryVector))
				.filter(result -> result != null)
				.sorted(Comparator.comparingDouble(RetrievedChunk::getScore).reversed())
				.limit(Math.max(1, limit))
				.toList();
	}

	private RetrievedChunk score(DocumentChunk chunk, double[] queryVector) {
		if (!StringUtils.hasText(chunk.getEmbeddingVector())) {
			return null;
		}
		var chunkVector = parseVector(chunk.getEmbeddingVector());
		if (chunkVector.length != queryVector.length) {
			return null;
		}
		return new RetrievedChunk(chunk, cosineSimilarity(queryVector, chunkVector));
	}

	private double[] parseVector(String json) {
		try {
			return objectMapper.readValue(json, double[].class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to parse embedding vector", e);
		}
	}

	private double cosineSimilarity(double[] left, double[] right) {
		double dot = 0.0;
		double leftMagnitude = 0.0;
		double rightMagnitude = 0.0;
		for (int i = 0; i < left.length; i++) {
			dot += left[i] * right[i];
			leftMagnitude += left[i] * left[i];
			rightMagnitude += right[i] * right[i];
		}
		if (leftMagnitude == 0.0 || rightMagnitude == 0.0) {
			return 0.0;
		}
		return dot / (Math.sqrt(leftMagnitude) * Math.sqrt(rightMagnitude));
	}
}
