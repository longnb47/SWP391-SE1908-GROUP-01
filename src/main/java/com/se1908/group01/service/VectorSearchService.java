package com.se1908.group01.service;

import com.se1908.group01.dto.RetrievedChunk;
import java.util.List;

public interface VectorSearchService {

	List<RetrievedChunk> search(Long documentId, String queryEmbeddingVector, int limit);
}
