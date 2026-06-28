package com.se1908.group01.service;

import com.se1908.group01.config.AiChatProperties;
import com.se1908.group01.dto.AiGenerationOptions;
import com.se1908.group01.enums.SupportedAiModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiGenerationOptionsService {

	private final AiChatProperties aiChatProperties;

	public AiGenerationOptionsService(AiChatProperties aiChatProperties) {
		this.aiChatProperties = aiChatProperties;
	}

	public AiGenerationOptions resolve(String requestedModel, Double requestedTemperature) {
		if (requestedModel != null && !StringUtils.hasText(requestedModel)) {
			throw new IllegalArgumentException("AI model must not be blank");
		}
		var modelName = StringUtils.hasText(requestedModel)
				? requestedModel.trim()
				: aiChatProperties.getDefaultModel();
		var model = SupportedAiModel.fromApiName(modelName);
		var temperature = requestedTemperature != null
				? requestedTemperature
				: aiChatProperties.getDefaultTemperature();

		if (!Double.isFinite(temperature) || temperature < 0.0 || temperature > 1.0) {
			throw new IllegalArgumentException("Temperature must be between 0.0 and 1.0");
		}

		return new AiGenerationOptions(model, temperature);
	}
}
