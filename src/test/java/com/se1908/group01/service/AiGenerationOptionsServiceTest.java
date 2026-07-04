package com.se1908.group01.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.se1908.group01.config.AiChatProperties;
import com.se1908.group01.enums.SupportedAiModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiGenerationOptionsServiceTest {

	private AiGenerationOptionsService optionsService;

	@BeforeEach
	void setUp() {
		var properties = new AiChatProperties();
		properties.setDefaultModel("gemini-2.5-flash-lite");
		properties.setDefaultTemperature(0.2);
		optionsService = new AiGenerationOptionsService(properties);
	}

	@Test
	void resolveUsesBackendDefaultsWhenRequestOptionsAreMissing() {
		var options = optionsService.resolve(null, null);

		assertEquals(SupportedAiModel.GEMINI_2_5_FLASH_LITE, options.model());
		assertEquals(0.2, options.temperature());
	}

	@Test
	void resolveAcceptsEverySupportedModel() {
		assertEquals(
				SupportedAiModel.GEMINI_2_5_FLASH_LITE,
				optionsService.resolve("gemini-2.5-flash-lite", 0.0).model()
		);
		assertEquals(
				SupportedAiModel.GEMINI_3_1_FLASH_LITE,
				optionsService.resolve("gemini-3.1-flash-lite", 0.5).model()
		);
		assertEquals(
				SupportedAiModel.GEMINI_3_5_FLASH,
				optionsService.resolve("gemini-3.5-flash", 1.0).model()
		);
	}

	@Test
	void resolveRejectsUnsupportedModel() {
		assertThrows(
				IllegalArgumentException.class,
				() -> optionsService.resolve("gemini-2.5-pro", 0.2)
		);
		assertThrows(
				IllegalArgumentException.class,
				() -> optionsService.resolve(" ", 0.2)
		);
	}

	@Test
	void resolveRejectsTemperatureOutsideAllowedRange() {
		assertThrows(
				IllegalArgumentException.class,
				() -> optionsService.resolve("gemini-2.5-flash-lite", -0.1)
		);
		assertThrows(
				IllegalArgumentException.class,
				() -> optionsService.resolve("gemini-2.5-flash-lite", 1.1)
		);
	}
}
