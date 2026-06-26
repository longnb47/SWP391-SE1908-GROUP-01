package com.se1908.group01.dto;

import com.se1908.group01.enums.KnowledgePolicy;
import java.util.List;

public class MultiChatAskResponse {

	private String answer;
	private String mode;
	private KnowledgePolicy policy;
	private List<Long> usedDocumentIds;

	public MultiChatAskResponse() {
	}

	public MultiChatAskResponse(String answer, String mode, KnowledgePolicy policy, List<Long> usedDocumentIds) {
		this.answer = answer;
		this.mode = mode;
		this.policy = policy;
		this.usedDocumentIds = usedDocumentIds;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public KnowledgePolicy getPolicy() {
		return policy;
	}

	public void setPolicy(KnowledgePolicy policy) {
		this.policy = policy;
	}

	public List<Long> getUsedDocumentIds() {
		return usedDocumentIds;
	}

	public void setUsedDocumentIds(List<Long> usedDocumentIds) {
		this.usedDocumentIds = usedDocumentIds;
	}
}
