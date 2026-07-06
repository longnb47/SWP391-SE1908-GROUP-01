package com.se1908.group01.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se1908.group01.config.S3Properties;
import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentStatus;
import com.se1908.group01.repository.ChatSessionDocumentRepository;
import com.se1908.group01.repository.DocumentChunkRepository;
import com.se1908.group01.repository.DocumentFolderRepository;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.repository.DocumentShareLinkRepository;
import com.se1908.group01.repository.DocumentShareRepository;
import com.se1908.group01.repository.DocumentTagRepository;
import com.se1908.group01.repository.FriendshipRepository;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.DocumentIngestionJobService;
import com.se1908.group01.service.DocumentIngestionService;
import com.se1908.group01.service.FileValidationService;
import com.se1908.group01.service.S3StorageService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplUploadTest {

	@Mock private FileValidationService fileValidationService;
	@Mock private S3StorageService s3StorageService;
	@Mock private S3Properties s3Properties;
	@Mock private DocumentRepository documentRepository;
	@Mock private DocumentFolderRepository documentFolderRepository;
	@Mock private DocumentChunkRepository documentChunkRepository;
	@Mock private DocumentTagRepository documentTagRepository;
	@Mock private DocumentIngestionService documentIngestionService;
	@Mock private DocumentShareLinkRepository documentShareLinkRepository;
	@Mock private DocumentShareRepository documentShareRepository;
	@Mock private FriendshipRepository friendshipRepository;
	@Mock private UserRepository userRepository;
	@Mock private DocumentIngestionJobService documentIngestionJobService;
	@Mock private CurrentUserService currentUserService;
	@Mock private ChatSessionDocumentRepository chatSessionDocumentRepository;

	private DocumentServiceImpl service;
	private MockMultipartFile file;

	@BeforeEach
	void setUp() {
		service = new DocumentServiceImpl(
				fileValidationService,
				s3StorageService,
				s3Properties,
				documentRepository,
				documentFolderRepository,
				documentChunkRepository,
				documentTagRepository,
				documentIngestionService,
				documentShareLinkRepository,
				documentShareRepository,
				friendshipRepository,
				userRepository,
				documentIngestionJobService,
				currentUserService,
				chatSessionDocumentRepository
		);
		file = new MockMultipartFile(
				"file",
				"..\\unsafe\r\nname.pdf",
				"application/pdf",
				"PDF content".getBytes(StandardCharsets.UTF_8)
		);
	}

	@Test
	void uploadStoresMetadataAndSchedulesIngestion() throws Exception {
		var tempFile = Path.of("document-ingestion-test.tmp");
		when(currentUserService.getCurrentUserId()).thenReturn(7L);
		when(s3Properties.getKeyPrefix()).thenReturn("study");
		when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
			Document document = invocation.getArgument(0);
			document.setDocumentId(11L);
			return document;
		});
		when(documentIngestionJobService.copyToTempFile(file)).thenReturn(tempFile);

		var response = service.upload(file, true);

		assertEquals(11L, response.getDocumentId());
		assertEquals(7L, response.getUserId());
		assertEquals("unsafe name.pdf", response.getOriginalFileName());
		assertEquals(true, response.getIsPublic());
		assertEquals(DocumentStatus.UPLOADED, response.getStatus());

		var keyCaptor = ArgumentCaptor.forClass(String.class);
		verify(s3StorageService).uploadPrivate(any(), keyCaptor.capture());
		assertTrue(keyCaptor.getValue().startsWith("study/documents/7/"));
		assertTrue(keyCaptor.getValue().endsWith("-unsafe name.pdf"));
		verify(documentIngestionJobService).ingestAsync(
				11L,
				tempFile,
				"unsafe name.pdf",
				"application/pdf"
		);
	}

	@Test
	void uploadDeletesS3ObjectWhenMetadataSaveFails() throws Exception {
		when(currentUserService.getCurrentUserId()).thenReturn(7L);
		when(documentRepository.save(any(Document.class)))
				.thenThrow(new IllegalStateException("Database unavailable"));

		assertThrows(IllegalStateException.class, () -> service.upload(file, false));

		var keyCaptor = ArgumentCaptor.forClass(String.class);
		verify(s3StorageService).uploadPrivate(any(), keyCaptor.capture());
		verify(s3StorageService).delete(keyCaptor.getValue());
		verify(documentIngestionJobService, never()).ingestAsync(any(), any(), anyString(), anyString());
	}

	@Test
	void uploadStopsBeforeDatabaseWhenS3UploadFails() throws Exception {
		when(currentUserService.getCurrentUserId()).thenReturn(7L);
		doThrow(new IOException("S3 unavailable"))
				.when(s3StorageService)
				.uploadPrivate(any(), anyString());

		assertThrows(IOException.class, () -> service.upload(file, false));

		verify(documentRepository, never()).save(any(Document.class));
		verify(documentIngestionJobService, never()).copyToTempFile(any());
	}
}
