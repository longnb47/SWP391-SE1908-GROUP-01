package com.se1908.group01.dto;

/** Request body used by the post-upload folder placement endpoint. */
public class DocumentMoveFolderRequest {

	private Long folderId;

	public Long getFolderId() {
		return folderId;
	}

	public void setFolderId(Long folderId) {
		this.folderId = folderId;
	}
}
