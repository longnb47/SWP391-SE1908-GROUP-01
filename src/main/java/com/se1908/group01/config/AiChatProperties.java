package com.se1908.group01.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public class AiChatProperties {

	private String defaultModel = "gemini-2.5-flash-lite";

	private double defaultTemperature = 0.2;

	public String getDefaultModel() {
		return defaultModel;
	}

	public void setDefaultModel(String defaultModel) {
		this.defaultModel = defaultModel;
	}

	public double getDefaultTemperature() {
		return defaultTemperature;
	}

	public void setDefaultTemperature(double defaultTemperature) {
		this.defaultTemperature = defaultTemperature;
	}
}
