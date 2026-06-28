package com.se1908.group01.dto;

import com.se1908.group01.enums.KnowledgePolicy;
import java.util.List;

public class MultiChatAskResponse {

	private String answer;
	private String mode;
	private KnowledgePolicy policy;
	private String model;
	private Double temperature;
	private List<Long> usedDocumentIds;

	public MultiChatAskResponse() {
	}

	public MultiChatAskResponse(
			String answer,
			String mode,
			KnowledgePolicy policy,
			String model,
			Double temperature,
			List<Long> usedDocumentIds
	) {
		this.answer = answer;
		this.mode = mode;
		this.policy = policy;
		this.model = model;
		this.temperature = temperature;
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

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	public List<Long> getUsedDocumentIds() {
		return usedDocumentIds;
	}

	public void setUsedDocumentIds(List<Long> usedDocumentIds) {
		this.usedDocumentIds = usedDocumentIds;
	}
}
