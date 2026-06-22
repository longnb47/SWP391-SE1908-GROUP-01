package com.se1908.group01.service.impl;

import com.se1908.group01.dto.MultiChatAskRequest;
import com.se1908.group01.dto.MultiChatAskResponse;
import com.se1908.group01.service.MultiChatService;
import org.springframework.stereotype.Service;

@Service
public class MultiChatServiceImpl implements MultiChatService {

	@Override
	public MultiChatAskResponse askMulti(MultiChatAskRequest request) {
		validateRequest(request);
		return new MultiChatAskResponse("Multi-document chat is not implemented yet.");
	}

	private void validateRequest(MultiChatAskRequest request) {
		if ("SelectedDocuments".equals(request.getMode())
				&& (request.getSelectedDocumentIds() == null || request.getSelectedDocumentIds().isEmpty())) {
			throw new IllegalArgumentException("selectedDocumentIds must be provided for SelectedDocuments mode");
		}
	}
}
