package com.se1908.group01.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public class AwsProperties {

	private String region;

	private String transcribeRegion;

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getTranscribeRegion() {
		return transcribeRegion;
	}

	public void setTranscribeRegion(String transcribeRegion) {
		this.transcribeRegion = transcribeRegion;
	}
}

