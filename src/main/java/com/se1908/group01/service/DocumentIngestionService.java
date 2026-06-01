package com.se1908.group01.service;

import com.se1908.group01.dto.ChunkData;
import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentChunk;
import com.se1908.group01.repository.DocumentChunkRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentIngestionService {

	private final DocumentParsingService parsingService;
	private final DocumentChunkingService chunkingService;
	private final DocumentEmbeddingService embeddingService;
	private final DocumentChunkRepository documentChunkRepository;

	public DocumentIngestionService(
			DocumentParsingService parsingService,
			DocumentChunkingService chunkingService,
			DocumentEmbeddingService embeddingService,
			DocumentChunkRepository documentChunkRepository
	) {
		this.parsingService = parsingService;
		this.chunkingService = chunkingService;
		this.embeddingService = embeddingService;
		this.documentChunkRepository = documentChunkRepository;
	}

	@Transactional(rollbackFor = Exception.class)
	public int ingest(Document document, MultipartFile file) throws IOException {
		if (document == null || document.getDocumentId() == null) {
			throw new IllegalArgumentException("Document is required");
		}

		var segments = parsingService.extractSegments(file);
		var chunks = chunkingService.chunk(segments);

		// Replace existing chunks for this document (if any).
		documentChunkRepository.deleteByDocumentDocumentId(document.getDocumentId());

		if (chunks.isEmpty()) {
			return 0;
		}

		var texts = chunks.stream().map(ChunkData::getContent).toList();
		var vectors = embeddingService.embedVectors(texts);
		if (vectors.size() != chunks.size()) {
			throw new IllegalStateException("Embedding vectors size mismatch");
		}

		List<DocumentChunk> entities = new ArrayList<>(chunks.size());
		for (int i = 0; i < chunks.size(); i++) {
			var c = chunks.get(i);
			var e = new DocumentChunk();
			e.setDocument(document);
			e.setChunkIndex(c.getChunkIndex());
			e.setPageNumber(c.getPageNumber());
			e.setContent(c.getContent());
			e.setEmbeddingVector(vectors.get(i));
			entities.add(e);
		}

		documentChunkRepository.saveAll(entities);
		return entities.size();
	}
}
