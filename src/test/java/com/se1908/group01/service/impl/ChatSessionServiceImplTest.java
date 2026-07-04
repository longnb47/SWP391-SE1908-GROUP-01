package com.se1908.group01.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se1908.group01.config.RagProperties;
import com.se1908.group01.dto.AiGenerationOptions;
import com.se1908.group01.dto.CreateChatSessionRequest;
import com.se1908.group01.entity.ChatSession;
import com.se1908.group01.entity.Document;
import com.se1908.group01.enums.ChatMode;
import com.se1908.group01.enums.KnowledgePolicy;
import com.se1908.group01.enums.SupportedAiModel;
import com.se1908.group01.repository.ChatMessageRepository;
import com.se1908.group01.repository.ChatMessageSourceRepository;
import com.se1908.group01.repository.ChatSessionDocumentRepository;
import com.se1908.group01.repository.ChatSessionRepository;
import com.se1908.group01.service.AiGenerationOptionsService;
import com.se1908.group01.service.ChatConversationMemoryService;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.DocumentAccessService;
import com.se1908.group01.service.DocumentEmbeddingService;
import com.se1908.group01.service.LlmClient;
import com.se1908.group01.service.PromptBuilderService;
import com.se1908.group01.service.VectorSearchService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatSessionServiceImplTest {

	@Mock
	private CurrentUserService currentUserService;
	@Mock
	private DocumentAccessService documentAccessService;
	@Mock
	private DocumentEmbeddingService documentEmbeddingService;
	@Mock
	private VectorSearchService vectorSearchService;
	@Mock
	private PromptBuilderService promptBuilderService;
	@Mock
	private LlmClient llmClient;
	@Mock
	private AiGenerationOptionsService aiGenerationOptionsService;
	@Mock
	private ChatConversationMemoryService chatConversationMemoryService;
	@Mock
	private ChatSessionRepository chatSessionRepository;
	@Mock
	private ChatSessionDocumentRepository chatSessionDocumentRepository;
	@Mock
	private ChatMessageRepository chatMessageRepository;
	@Mock
	private ChatMessageSourceRepository chatMessageSourceRepository;

	private ChatSessionServiceImpl chatSessionService;

	@BeforeEach
	void setUp() {
		chatSessionService = new ChatSessionServiceImpl(
				currentUserService,
				documentAccessService,
				documentEmbeddingService,
				vectorSearchService,
				promptBuilderService,
				llmClient,
				aiGenerationOptionsService,
				chatConversationMemoryService,
				chatSessionRepository,
				chatSessionDocumentRepository,
				chatMessageRepository,
				chatMessageSourceRepository,
				new RagProperties()
		);
	}

	@Test
	void createSelectedSessionPersistsResolvedDocumentsAndGenerationOptions() {
		var document = new Document();
		document.setDocumentId(10L);
		var request = new CreateChatSessionRequest(
				"Study session",
				"SelectedDocuments",
				List.of(10L),
				null,
				true,
				"gemini-3.1-flash-lite",
				0.3
		);
		when(currentUserService.getCurrentUserId()).thenReturn(1L);
		when(documentAccessService.getReadyDocumentsForChat(1L, List.of(10L)))
				.thenReturn(List.of(document));
		when(aiGenerationOptionsService.resolve("gemini-3.1-flash-lite", 0.3))
				.thenReturn(new AiGenerationOptions(SupportedAiModel.GEMINI_3_1_FLASH_LITE, 0.3));
		when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> {
			ChatSession session = invocation.getArgument(0);
			session.setSessionId(5L);
			return session;
		});
		when(chatSessionDocumentRepository.findDocumentIdsBySessionId(5L))
				.thenReturn(List.of(10L));

		var response = chatSessionService.createSession(request);

		assertEquals(5L, response.sessionId());
		assertEquals(ChatMode.SELECTED_DOCUMENTS, response.mode());
		assertEquals(KnowledgePolicy.DOCUMENTS_ONLY, response.policy());
		assertEquals("gemini-3.1-flash-lite", response.model());
		assertEquals(0.3, response.temperature());
		assertEquals(List.of(10L), response.selectedDocumentIds());
		verify(chatSessionDocumentRepository).save(any());
	}
}
