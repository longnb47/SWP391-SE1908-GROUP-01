package com.se1908.group01.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se1908.group01.dto.AiGenerationOptions;
import com.se1908.group01.dto.MultiChatAskRequest;
import com.se1908.group01.enums.SupportedAiModel;
import com.se1908.group01.service.AiGenerationOptionsService;
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

	@Mock
	private AiGenerationOptionsService aiGenerationOptionsService;

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
				aiGenerationOptionsService
		);
		when(currentUserService.getCurrentUserId()).thenReturn(1L);
		when(aiGenerationOptionsService.resolve(null, null))
				.thenReturn(new AiGenerationOptions(SupportedAiModel.GEMINI_2_5_FLASH_LITE, 0.2));
	}

	@Test
	void userStorageSearchesOwnedDocumentsOnlyWhenNoFolderProvided() {
		var request = request("UserStorage", null);
		when(documentAccessService.getAllReadyDocumentsForUser(1L, null, false))
				.thenReturn(List.of());

		var response = multiChatService.askMulti(request);

		assertEquals("USER_STORAGE", response.getMode());
		assertEquals("gemini-2.5-flash-lite", response.getModel());
		assertEquals(0.2, response.getTemperature());
		assertEquals(
				"I cannot find sufficient information in your documents to answer this question.",
				response.getAnswer()
		);
		verify(documentAccessService).getAllReadyDocumentsForUser(1L, null, false);
	}

	@Test
	void userStorageScopesToFolderWhenFolderIdProvided() {
		var request = request("UserStorage", 42L);
		when(documentAccessService.getAllReadyDocumentsForUser(1L, 42L, false))
				.thenReturn(List.of());

		var response = multiChatService.askMulti(request);

		assertEquals(
				"I cannot find sufficient information in this folder to answer this question.",
				response.getAnswer()
		);
		verify(documentAccessService).getAllReadyDocumentsForUser(1L, 42L, false);
	}

	@Test
	void selectedDocumentsResolvesScopeBySelectedIdsOnly() {
		var request = request("SelectedDocuments", null);
		request.setSelectedDocumentIds(List.of(10L, 20L));
		when(documentAccessService.getReadyDocumentsForChat(1L, request.getSelectedDocumentIds()))
				.thenReturn(List.of());

		var response = multiChatService.askMulti(request);

		assertEquals("SELECTED_DOCUMENTS", response.getMode());
		assertEquals(
				"I cannot find this information in the documents you selected.",
				response.getAnswer()
		);
		verify(documentAccessService).getReadyDocumentsForChat(1L, List.of(10L, 20L));
	}

	private MultiChatAskRequest request(String mode, Long folderId) {
		var request = new MultiChatAskRequest();
		request.setMode(mode);
		request.setQuestion("What is this about?");
		request.setFolderId(folderId);
		return request;
	}
}
