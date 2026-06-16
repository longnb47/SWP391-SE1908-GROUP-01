package com.se1908.group01.config;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties({AwsProperties.class, S3Properties.class})
public class S3Config {

	@Bean
	@ConditionalOnExpression(
			"T(org.springframework.util.StringUtils).hasText('${aws.region:}')"
					+ " and T(org.springframework.util.StringUtils).hasText('${aws.s3.bucket-name:}')"
	)
	S3Client s3Client(AwsProperties awsProperties, S3Properties s3Properties) {
		var region = awsProperties.getRegion();
		var endpoint = s3Properties.getEndpoint();

		var builder = S3Client.builder()
				.region(Region.of(region))
				.credentialsProvider(EnvironmentVariableCredentialsProvider.create());

		if (StringUtils.hasText(endpoint)) {
			builder = builder
					.endpointOverride(URI.create(endpoint))
					.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
		}

		return builder.build();
	}

	@Bean
	@ConditionalOnExpression(
			"T(org.springframework.util.StringUtils).hasText('${aws.region:}')"
					+ " and T(org.springframework.util.StringUtils).hasText('${aws.s3.bucket-name:}')"
	)
	S3Presigner s3Presigner(AwsProperties awsProperties, S3Properties s3Properties) {
		var region = awsProperties.getRegion();
		var endpoint = s3Properties.getEndpoint();

		var builder = S3Presigner.builder()
				.region(Region.of(region))
				.credentialsProvider(EnvironmentVariableCredentialsProvider.create());

		if (StringUtils.hasText(endpoint)) {
			builder = builder
					.endpointOverride(URI.create(endpoint))
					.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
		}

		return builder.build();
	}
}
