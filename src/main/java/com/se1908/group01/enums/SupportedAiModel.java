package com.se1908.group01.enums;

import java.util.Arrays;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;

public enum SupportedAiModel {

	GEMINI_2_5_FLASH_LITE(
			"gemini-2.5-flash-lite",
			GoogleGenAiChatModel.ChatModel.GEMINI_2_5_FLASH_LIGHT
	),
	GEMINI_3_1_FLASH_LITE(
			"gemini-3.1-flash-lite",
			GoogleGenAiChatModel.ChatModel.GEMINI_3_1_FLASH_LITE
	),
	GEMINI_3_5_FLASH(
			"gemini-3.5-flash",
			GoogleGenAiChatModel.ChatModel.GEMINI_3_5_FLASH
	);

	private final String apiName;
	private final GoogleGenAiChatModel.ChatModel providerModel;

	SupportedAiModel(String apiName, GoogleGenAiChatModel.ChatModel providerModel) {
		this.apiName = apiName;
		this.providerModel = providerModel;
	}

	public String getApiName() {
		return apiName;
	}

	public GoogleGenAiChatModel.ChatModel getProviderModel() {
		return providerModel;
	}

	public static SupportedAiModel fromApiName(String apiName) {
		return Arrays.stream(values())
				.filter(model -> model.apiName.equals(apiName))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(
						"Unsupported AI model. Allowed models: "
								+ String.join(", ", allowedApiNames())
				));
	}

	public static String[] allowedApiNames() {
		return Arrays.stream(values())
				.map(SupportedAiModel::getApiName)
				.toArray(String[]::new);
	}
}
