package com.se1908.group01.service.impl;

import com.se1908.group01.dto.ChunkData;
import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentChunk;
import com.se1908.group01.entity.DocumentStatus;
import com.se1908.group01.repository.DocumentChunkRepository;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.service.DocumentChunkingService;
import com.se1908.group01.service.DocumentEmbeddingService;
import com.se1908.group01.service.DocumentIngestionService;
import com.se1908.group01.service.DocumentParsingService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentIngestionServiceImpl implements DocumentIngestionService {

	private final DocumentParsingService parsingService;
	private final DocumentChunkingService chunkingService;
	private final DocumentEmbeddingService embeddingService;
	private final DocumentChunkRepository documentChunkRepository;
	private final DocumentRepository documentRepository;

	public DocumentIngestionServiceImpl(
			DocumentParsingService parsingService,
			DocumentChunkingService chunkingService,
			DocumentEmbeddingService embeddingService,
			DocumentChunkRepository documentChunkRepository,
			DocumentRepository documentRepository
	) {
		this.parsingService = parsingService;
		this.chunkingService = chunkingService;
		this.embeddingService = embeddingService;
		this.documentChunkRepository = documentChunkRepository;
		this.documentRepository = documentRepository;
	}

	@Override
	public int ingest(Document document, MultipartFile file) throws IOException {
		if (document == null || document.getDocumentId() == null) {
			throw new IllegalArgumentException("Document is required");
		}

		updateStatus(document, DocumentStatus.PARSING);
		var segments = parsingService.extractSegments(file, document);
		var chunks = chunkingService.chunk(segments);

		// Replace existing chunks for this document (if any).
		documentChunkRepository.deleteByDocumentDocumentId(document.getDocumentId());

		if (chunks.isEmpty()) {
			updateStatus(document, DocumentStatus.FAILED);
			throw new IllegalStateException("No text content could be extracted from document");
		}

		updateStatus(document, DocumentStatus.INDEXING);
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
		updateStatus(document, DocumentStatus.READY);
		return entities.size();
	}

	private void updateStatus(Document document, DocumentStatus status) {
		document.setStatus(status);
		documentRepository.save(document);
	}
}
