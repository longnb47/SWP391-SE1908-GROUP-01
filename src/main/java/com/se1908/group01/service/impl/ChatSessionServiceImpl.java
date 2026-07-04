package com.se1908.group01.service.impl;

import com.se1908.group01.config.RagProperties;
import com.se1908.group01.dto.ChatMessageListResponse;
import com.se1908.group01.dto.ChatMessageResponse;
import com.se1908.group01.dto.ChatMessageSourceResponse;
import com.se1908.group01.dto.ChatSessionResponse;
import com.se1908.group01.dto.CreateChatSessionRequest;
import com.se1908.group01.dto.RetrievedChunk;
import com.se1908.group01.dto.SendChatMessageRequest;
import com.se1908.group01.dto.UpdateChatSessionRequest;
import com.se1908.group01.entity.ChatMessage;
import com.se1908.group01.entity.ChatMessageSource;
import com.se1908.group01.entity.ChatSession;
import com.se1908.group01.entity.ChatSessionDocument;
import com.se1908.group01.entity.ChatSessionDocumentId;
import com.se1908.group01.entity.Document;
import com.se1908.group01.enums.ChatMessageRole;
import com.se1908.group01.enums.ChatMessageStatus;
import com.se1908.group01.enums.ChatMode;
import com.se1908.group01.enums.KnowledgePolicy;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.ChatMessageRepository;
import com.se1908.group01.repository.ChatMessageSourceRepository;
import com.se1908.group01.repository.ChatSessionDocumentRepository;
import com.se1908.group01.repository.ChatSessionRepository;
import com.se1908.group01.service.AiGenerationOptionsService;
import com.se1908.group01.service.ChatConversationMemoryService;
import com.se1908.group01.service.ChatSessionService;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.DocumentAccessService;
import com.se1908.group01.service.DocumentEmbeddingService;
import com.se1908.group01.service.LlmClient;
import com.se1908.group01.service.PromptBuilderService;
import com.se1908.group01.service.VectorSearchService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ChatSessionServiceImpl implements ChatSessionService {

	private static final int TOP_K = 10;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String DEFAULT_TITLE = "New chat";

	private final CurrentUserService currentUserService;
	private final DocumentAccessService documentAccessService;
	private final DocumentEmbeddingService documentEmbeddingService;
	private final VectorSearchService vectorSearchService;
	private final PromptBuilderService promptBuilderService;
	private final LlmClient llmClient;
	private final AiGenerationOptionsService aiGenerationOptionsService;
	private final ChatConversationMemoryService chatConversationMemoryService;
	private final ChatSessionRepository chatSessionRepository;
	private final ChatSessionDocumentRepository chatSessionDocumentRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatMessageSourceRepository chatMessageSourceRepository;
	private final RagProperties ragProperties;

	public ChatSessionServiceImpl(
			CurrentUserService currentUserService,
			DocumentAccessService documentAccessService,
			DocumentEmbeddingService documentEmbeddingService,
			VectorSearchService vectorSearchService,
			PromptBuilderService promptBuilderService,
			LlmClient llmClient,
			AiGenerationOptionsService aiGenerationOptionsService,
			ChatConversationMemoryService chatConversationMemoryService,
			ChatSessionRepository chatSessionRepository,
			ChatSessionDocumentRepository chatSessionDocumentRepository,
			ChatMessageRepository chatMessageRepository,
			ChatMessageSourceRepository chatMessageSourceRepository,
			RagProperties ragProperties
	) {
		this.currentUserService = currentUserService;
		this.documentAccessService = documentAccessService;
		this.documentEmbeddingService = documentEmbeddingService;
		this.vectorSearchService = vectorSearchService;
		this.promptBuilderService = promptBuilderService;
		this.llmClient = llmClient;
		this.aiGenerationOptionsService = aiGenerationOptionsService;
		this.chatConversationMemoryService = chatConversationMemoryService;
		this.chatSessionRepository = chatSessionRepository;
		this.chatSessionDocumentRepository = chatSessionDocumentRepository;
		this.chatMessageRepository = chatMessageRepository;
		this.chatMessageSourceRepository = chatMessageSourceRepository;
		this.ragProperties = ragProperties;
	}

	@Transactional
	@Override
	public ChatSessionResponse createSession(CreateChatSessionRequest request) {
		var userId = currentUserService.getCurrentUserId();
		var mode = resolveMode(request.mode());
		var policy = resolvePolicy(mode, request.useGeneralKnowledge());
		var options = aiGenerationOptionsService.resolve(request.model(), request.temperature());
		List<Document> selectedDocuments = List.of();

		if (mode == ChatMode.SELECTED_DOCUMENTS) {
			if (request.folderId() != null) {
				throw new IllegalArgumentException("folderId is not supported in SelectedDocuments mode");
			}
			selectedDocuments = documentAccessService.getReadyDocumentsForChat(
					userId,
					request.selectedDocumentIds()
			);
		} else {
			if (request.selectedDocumentIds() != null && !request.selectedDocumentIds().isEmpty()) {
				throw new IllegalArgumentException("selectedDocumentIds is not supported in UserStorage mode");
			}
			documentAccessService.getAllReadyDocumentsForUser(
					userId,
					request.folderId(),
					policy == KnowledgePolicy.DOCUMENTS_PLUS_GENERAL
			);
		}

		var session = new ChatSession();
		session.setUserId(userId);
		session.setTitle(normalizeTitle(request.title()));
		session.setChatMode(mode);
		session.setFolderId(request.folderId());
		session.setKnowledgePolicy(policy);
		session.setModel(options.modelName());
		session.setTemperature(options.temperature());
		session = chatSessionRepository.save(session);

		for (var document : selectedDocuments) {
			var link = new ChatSessionDocument();
			link.setId(new ChatSessionDocumentId(session.getSessionId(), document.getDocumentId()));
			link.setChatSession(session);
			link.setDocument(document);
			chatSessionDocumentRepository.save(link);
		}

		return toSessionResponse(session);
	}

	@Transactional(readOnly = true)
	@Override
	public List<ChatSessionResponse> getMySessions() {
		var userId = currentUserService.getCurrentUserId();
		return chatSessionRepository.findByUserIdAndIsDeletedFalseOrderByUpdatedAtDesc(userId)
				.stream()
				.map(this::toSessionResponse)
				.toList();
	}

	@Transactional
	@Override
	public ChatSessionResponse updateSession(Long sessionId, UpdateChatSessionRequest request) {
		var session = findOwnedSession(sessionId);
		session.setTitle(normalizeTitle(request.title()));
		return toSessionResponse(chatSessionRepository.save(session));
	}

	@Transactional
	@Override
	public void deleteSession(Long sessionId) {
		var session = findOwnedSession(sessionId);
		session.setIsDeleted(Boolean.TRUE);
		chatSessionRepository.save(session);
	}

	@Transactional(readOnly = true)
	@Override
	public ChatMessageListResponse getMessages(Long sessionId, int page, int size) {
		var session = findOwnedSession(sessionId);
		if (page < 0) {
			throw new IllegalArgumentException("Page must be greater than or equal to 0");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Size must be between 1 and " + MAX_PAGE_SIZE);
		}

		var messagePage = chatMessageRepository.findByChatSessionSessionIdOrderByCreatedAtAscMessageIdAsc(
				session.getSessionId(),
				PageRequest.of(page, size)
		);
		var messageIds = messagePage.getContent().stream()
				.map(ChatMessage::getMessageId)
				.toList();
		Map<Long, List<ChatMessageSourceResponse>> sourcesByMessage = loadSources(messageIds);
		var messages = messagePage.getContent().stream()
				.map(message -> toMessageResponse(
						message,
						sourcesByMessage.getOrDefault(message.getMessageId(), List.of())
				))
				.toList();

		return new ChatMessageListResponse(
				messages,
				messagePage.getNumber(),
				messagePage.getSize(),
				messagePage.getTotalElements(),
				messagePage.getTotalPages()
		);
	}

	@Override
	public ChatMessageResponse sendMessage(Long sessionId, SendChatMessageRequest request) {
		var session = findOwnedSession(sessionId);
		var conversationMemory = chatConversationMemoryService.getRecentMessages(sessionId);
		saveMessage(
				session,
				ChatMessageRole.USER,
				request.question().trim(),
				ChatMessageStatus.COMPLETED
		);

		try {
			var documents = resolveDocuments(session);
			var options = aiGenerationOptionsService.resolve(session.getModel(), session.getTemperature());
			if (documents.isEmpty()) {
				return saveNoContextAnswer(session);
			}

			var resolvedDocumentIds = documents.stream()
					.map(Document::getDocumentId)
					.toList();
			var questionEmbedding = documentEmbeddingService.embedQuestion(request.question());
			var chunks = vectorSearchService.search(
					questionEmbedding,
					resolvedDocumentIds,
					session.getUserId(),
					session.getFolderId(),
					TOP_K
			);
			if (chunks.isEmpty()) {
				return saveNoContextAnswer(session);
			}

			var prompt = promptBuilderService.buildSessionQuestionPrompt(
					session.getChatMode(),
					session.getKnowledgePolicy(),
					buildContext(chunks),
					conversationMemory,
					request.question()
			);
			var answer = llmClient.generateAnswer(prompt, options);
			var assistantMessage = saveMessage(
					session,
					ChatMessageRole.ASSISTANT,
					answer,
					ChatMessageStatus.COMPLETED
			);
			var sources = saveSources(assistantMessage, chunks);
			touchSession(session);
			return toMessageResponse(assistantMessage, sources);
		} catch (RuntimeException ex) {
			saveMessage(
					session,
					ChatMessageRole.ASSISTANT,
					"AI service could not generate a response.",
					ChatMessageStatus.FAILED
			);
			touchSession(session);
			throw ex;
		}
	}

	private List<Document> resolveDocuments(ChatSession session) {
		if (session.getChatMode() == ChatMode.SELECTED_DOCUMENTS) {
			var documentIds = chatSessionDocumentRepository.findDocumentIdsBySessionId(session.getSessionId());
			if (documentIds.isEmpty()) {
				return List.of();
			}
			return documentAccessService.getReadyDocumentsForChat(session.getUserId(), documentIds);
		}
		return documentAccessService.getAllReadyDocumentsForUser(
				session.getUserId(),
				session.getFolderId(),
				session.getKnowledgePolicy() == KnowledgePolicy.DOCUMENTS_PLUS_GENERAL
		);
	}

	private ChatMessageResponse saveNoContextAnswer(ChatSession session) {
		var answer = noContextMessage(session.getChatMode(), session.getKnowledgePolicy());
		var assistantMessage = saveMessage(
				session,
				ChatMessageRole.ASSISTANT,
				answer,
				ChatMessageStatus.COMPLETED
		);
		touchSession(session);
		return toMessageResponse(assistantMessage, List.of());
	}

	private ChatMessage saveMessage(
			ChatSession session,
			ChatMessageRole role,
			String content,
			ChatMessageStatus status
	) {
		var message = new ChatMessage();
		message.setChatSession(session);
		message.setRole(role);
		message.setContent(content);
		message.setStatus(status);
		return chatMessageRepository.save(message);
	}

	private List<ChatMessageSourceResponse> saveSources(
			ChatMessage message,
			List<RetrievedChunk> chunks
	) {
		List<ChatMessageSource> entities = new ArrayList<>();
		for (var retrieved : chunks) {
			var chunk = retrieved.getChunk();
			var source = new ChatMessageSource();
			source.setMessage(message);
			source.setDocumentId(chunk.getDocument().getDocumentId());
			source.setChunkId(chunk.getChunkId());
			source.setPageNumber(chunk.getPageNumber());
			source.setSimilarityScore(roundScore(retrieved.getScore()));
			entities.add(source);
		}
		chatMessageSourceRepository.saveAll(entities);
		return entities.stream()
				.map(this::toSourceResponse)
				.toList();
	}

	private Map<Long, List<ChatMessageSourceResponse>> loadSources(List<Long> messageIds) {
		Map<Long, List<ChatMessageSourceResponse>> result = new HashMap<>();
		if (messageIds.isEmpty()) {
			return result;
		}
		for (var source : chatMessageSourceRepository.findByMessageMessageIdIn(messageIds)) {
			result.computeIfAbsent(source.getMessage().getMessageId(), ignored -> new ArrayList<>())
					.add(toSourceResponse(source));
		}
		return result;
	}

	private ChatMessageResponse toMessageResponse(
			ChatMessage message,
			List<ChatMessageSourceResponse> sources
	) {
		return new ChatMessageResponse(
				message.getMessageId(),
				message.getRole(),
				message.getContent(),
				message.getStatus(),
				message.getCreatedAt(),
				sources
		);
	}

	private ChatMessageSourceResponse toSourceResponse(ChatMessageSource source) {
		return new ChatMessageSourceResponse(
				source.getDocumentId(),
				source.getChunkId(),
				source.getPageNumber(),
				source.getSimilarityScore()
		);
	}

	private ChatSessionResponse toSessionResponse(ChatSession session) {
		return new ChatSessionResponse(
				session.getSessionId(),
				session.getTitle(),
				session.getChatMode(),
				session.getFolderId(),
				session.getKnowledgePolicy(),
				session.getModel(),
				session.getTemperature(),
				chatSessionDocumentRepository.findDocumentIdsBySessionId(session.getSessionId()),
				session.getCreatedAt(),
				session.getUpdatedAt()
		);
	}

	private ChatSession findOwnedSession(Long sessionId) {
		var userId = currentUserService.getCurrentUserId();
		return chatSessionRepository.findBySessionIdAndUserIdAndIsDeletedFalse(sessionId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
	}

	private ChatMode resolveMode(String mode) {
		return switch (mode) {
			case "SelectedDocuments" -> ChatMode.SELECTED_DOCUMENTS;
			case "UserStorage" -> ChatMode.USER_STORAGE;
			default -> throw new IllegalArgumentException("Unsupported chat mode");
		};
	}

	private KnowledgePolicy resolvePolicy(ChatMode mode, Boolean useGeneralKnowledge) {
		if (mode == ChatMode.SELECTED_DOCUMENTS) {
			return KnowledgePolicy.DOCUMENTS_ONLY;
		}
		var includePublicDocuments = useGeneralKnowledge != null
				? useGeneralKnowledge
				: ragProperties.getUserStorage().isAllowGeneralKnowledge();
		return includePublicDocuments
				? KnowledgePolicy.DOCUMENTS_PLUS_GENERAL
				: KnowledgePolicy.DOCUMENTS_ONLY;
	}

	private String normalizeTitle(String title) {
		return StringUtils.hasText(title) ? title.trim() : DEFAULT_TITLE;
	}

	private void touchSession(ChatSession session) {
		session.setUpdatedAt(Instant.now());
		chatSessionRepository.save(session);
	}

	private String buildContext(List<RetrievedChunk> chunks) {
		var context = new StringBuilder();
		for (var retrieved : chunks) {
			var chunk = retrieved.getChunk();
			context.append("[Document ")
					.append(chunk.getDocument().getDocumentId())
					.append(", chunk ")
					.append(chunk.getChunkIndex())
					.append("]\n")
					.append(chunk.getContent())
					.append("\n\n");
		}
		return context.toString().trim();
	}

	private String noContextMessage(ChatMode mode, KnowledgePolicy policy) {
		return switch (mode) {
			case SELECTED_DOCUMENTS ->
					"I cannot find this information in the documents you selected.";
			case USER_STORAGE -> policy == KnowledgePolicy.DOCUMENTS_PLUS_GENERAL
					? "I cannot find sufficient information in your documents or public documents to answer this question."
					: "I cannot find sufficient information in your documents to answer this question.";
		};
	}

	private double roundScore(double score) {
		return Math.round(score * 10000.0) / 10000.0;
	}
}
