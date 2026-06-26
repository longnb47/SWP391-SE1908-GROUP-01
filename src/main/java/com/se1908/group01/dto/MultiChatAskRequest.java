package com.se1908.group01.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public class MultiChatAskRequest {

	@NotNull(message = "Mode is required")
	@Pattern(regexp = "SelectedDocuments|UserStorage", message = "mode must be 'SelectedDocuments' or 'UserStorage'")
	private String mode;

	private List<Long> selectedDocumentIds;

	private Long folderId;

	@NotBlank(message = "Question is required")
	private String question;

	private Boolean useGeneralKnowledge;

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public List<Long> getSelectedDocumentIds() {
		return selectedDocumentIds;
	}

	public void setSelectedDocumentIds(List<Long> selectedDocumentIds) {
		this.selectedDocumentIds = selectedDocumentIds;
	}

	public Long getFolderId() {
		return folderId;
	}

	public void setFolderId(Long folderId) {
		this.folderId = folderId;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public Boolean getUseGeneralKnowledge() {
		return useGeneralKnowledge;
	}

	public void setUseGeneralKnowledge(Boolean useGeneralKnowledge) {
		this.useGeneralKnowledge = useGeneralKnowledge;
	}
}
