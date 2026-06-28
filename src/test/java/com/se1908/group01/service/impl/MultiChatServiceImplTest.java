package com.se1908.group01.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se1908.group01.config.RagProperties;
import com.se1908.group01.dto.MultiChatAskRequest;
import com.se1908.group01.enums.KnowledgePolicy;
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
class MultiChatServiceImplTest {

	@Mock
	private DocumentAccessService documentAccessService;

	@Mock
	private CurrentUserService currentUserService;

	@Mock
	private DocumentEmbeddingService documentEmbeddingService;

	@Mock
	private VectorSearchService vectorSearchService;

	@Mock
	private PromptBuilderService promptBuilderService;

	@Mock
	private LlmClient llmClient;

	private MultiChatServiceImpl multiChatService;

	@BeforeEach
	void setUp() {
		multiChatService = new MultiChatServiceImpl(
				documentAccessService,
				currentUserService,
				documentEmbeddingService,
				vectorSearchService,
				promptBuilderService,
				llmClient,
				new RagProperties()
		);
		when(currentUserService.getCurrentUserId()).thenReturn(1L);
	}

	@Test
	void userStorageExcludesPublicDocumentsWhenGeneralKnowledgeIsDisabled() {
		var request = request("UserStorage", false);
		when(documentAccessService.getAllReadyDocumentsForUser(1L, null, false))
				.thenReturn(List.of());

		var response = multiChatService.askMulti(request);

		assertEquals(KnowledgePolicy.DOCUMENTS_ONLY, response.getPolicy());
		verify(documentAccessService).getAllReadyDocumentsForUser(1L, null, false);
	}

	@Test
	void userStorageIncludesPublicDocumentsWhenGeneralKnowledgeIsEnabled() {
		var request = request("UserStorage", true);
		when(documentAccessService.getAllReadyDocumentsForUser(1L, null, true))
				.thenReturn(List.of());

		var response = multiChatService.askMulti(request);

		assertEquals(KnowledgePolicy.DOCUMENTS_PLUS_GENERAL, response.getPolicy());
		verify(documentAccessService).getAllReadyDocumentsForUser(1L, null, true);
	}

	@Test
	void selectedDocumentsAlwaysUseDocumentsOnlyPolicy() {
		var request = request("SelectedDocuments", true);
		request.setSelectedDocumentIds(List.of(10L, 20L));
		when(documentAccessService.getReadyDocumentsForChat(1L, request.getSelectedDocumentIds()))
				.thenReturn(List.of());

		var response = multiChatService.askMulti(request);

		assertEquals(KnowledgePolicy.DOCUMENTS_ONLY, response.getPolicy());
		verify(documentAccessService).getReadyDocumentsForChat(1L, List.of(10L, 20L));
	}

	private MultiChatAskRequest request(String mode, Boolean useGeneralKnowledge) {
		var request = new MultiChatAskRequest();
		request.setMode(mode);
		request.setQuestion("What is this about?");
		request.setUseGeneralKnowledge(useGeneralKnowledge);
		return request;
	}
}
