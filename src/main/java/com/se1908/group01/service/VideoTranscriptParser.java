package com.se1908.group01.service;

import com.google.cloud.speech.v1.LongRunningRecognizeRequest;
import com.google.cloud.speech.v1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.se1908.group01.config.GoogleSpeechProperties;
import com.se1908.group01.config.S3Properties;
import com.se1908.group01.entity.Document;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Service
public class VideoTranscriptParser {

    private static final Logger log = LoggerFactory.getLogger(VideoTranscriptParser.class);

    private final SpeechClient speechClient;
    private final Storage storage;
    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final GoogleSpeechProperties googleSpeechProperties;

    public VideoTranscriptParser(
            ObjectProvider<SpeechClient> speechClientProvider,
            ObjectProvider<Storage> storageProvider,
            ObjectProvider<S3Client> s3ClientProvider,
            S3Properties s3Properties,
            GoogleSpeechProperties googleSpeechProperties
    ) {
        this.speechClient = speechClientProvider.getIfAvailable();
        this.storage = storageProvider.getIfAvailable();
        this.s3Client = s3ClientProvider.getIfAvailable();
        this.s3Properties = s3Properties;
        this.googleSpeechProperties = googleSpeechProperties;
    }

    public String parse(Long documentId, String s3Key, String contentType) {
        if (speechClient == null || storage == null) {
            throw new IllegalStateException(
                    "Google Cloud Speech-to-Text is not configured. "
                            + "Set GOOGLE_SPEECH_GCS_BUCKET and GOOGLE_APPLICATION_CREDENTIALS environment variables.");
        }
        if (s3Client == null) {
            throw new IllegalStateException(
                    "AWS S3 is not configured. Set AWS_REGION and AWS_S3_BUCKET_NAME environment variables.");
        }

        Path tempMp4 = null;
        Path tempFlac = null;
        String flacGcsKey = null;

        try {
            // 1. Download MP4 from S3 → local temp file
            tempMp4 = Files.createTempFile("video-" + documentId + "-", ".mp4");
            log.info("Downloading video from S3 for document {}", documentId);
            var videoBytes = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(s3Properties.getBucketName())
                            .key(s3Key)
                            .build()
            ).asByteArray();
            Files.write(tempMp4, videoBytes);

            // 2. Extract audio to FLAC (16 kHz mono) via system FFmpeg
            tempFlac = Files.createTempFile("audio-" + documentId + "-", ".flac");
            extractAudio(tempMp4, tempFlac);

            // 3. Upload FLAC to GCS
            flacGcsKey = googleSpeechProperties.getGcsKeyPrefix()
                    + documentId + "/audio-" + System.currentTimeMillis() + ".flac";
            log.info("Uploading FLAC to GCS: {}", flacGcsKey);
            storage.create(
                    BlobInfo.newBuilder(BlobId.of(googleSpeechProperties.getGcsBucketName(), flacGcsKey))
                            .setContentType("audio/flac")
                            .build(),
                    Files.readAllBytes(tempFlac)
            );

            // 4. Transcribe via Google STT v1
            return transcribe(documentId, flacGcsKey);

        } catch (IOException e) {
            throw new RuntimeException(
                    "I/O error during video transcription for document " + documentId, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Video transcription interrupted for document " + documentId, e);
        } finally {
            // 5. Always clean up local temp files and the GCS audio object
            deleteTempFile(tempMp4);
            deleteTempFile(tempFlac);
            deleteFromGcs(flacGcsKey);
        }
    }

    private void extractAudio(Path inputMp4, Path outputFlac) throws IOException, InterruptedException {
        log.debug("FFmpeg: extracting audio {} → {}", inputMp4, outputFlac);
        var process = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", inputMp4.toAbsolutePath().toString(),
                "-vn",             // strip video stream
                "-acodec", "flac", // FLAC codec
                "-ar", "16000",    // 16 kHz sample rate
                "-ac", "1",        // mono
                outputFlac.toAbsolutePath().toString()
        )
                .redirectErrorStream(true) // merge stderr into stdout so we can drain it
                .start();

        // Drain output to prevent pipe-buffer deadlock on large files
        var ffmpegOutput = new String(process.getInputStream().readAllBytes());
        var exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(
                    "FFmpeg exited with code " + exitCode + " for document input " + inputMp4
                            + ":\n" + ffmpegOutput);
        }
        log.debug("FFmpeg extraction complete, output: {}", outputFlac);
    }

    private String transcribe(Long documentId, String flacGcsKey) {
        var gcsUri = "gs://" + googleSpeechProperties.getGcsBucketName() + "/" + flacGcsKey;
        log.info("Calling STT LongRunningRecognize for document {}, uri={}", documentId, gcsUri);

        // No setEncoding — FLAC from GCS is auto-detected by STT v1
        var config = RecognitionConfig.newBuilder()
                .setLanguageCode("en-US")
                .build();

        var audio = RecognitionAudio.newBuilder()
                .setUri(gcsUri)
                .build();

        var request = LongRunningRecognizeRequest.newBuilder()
                .setConfig(config)
                .setAudio(audio)
                .build();

        LongRunningRecognizeResponse response;
        try {
            response = speechClient.longRunningRecognizeAsync(request)
                    .get(googleSpeechProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Transcription interrupted for document " + documentId, e);
        } catch (TimeoutException e) {
            throw new RuntimeException("Transcription timed out after "
                    + googleSpeechProperties.getTimeoutSeconds() + "s for document "
                    + documentId, e);
        } catch (ExecutionException e) {
            var cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException(
                    "Transcription failed for document " + documentId
                            + ": " + cause.getMessage(), cause);
        }

        var sb = new StringBuilder();
        for (var result : response.getResultsList()) {
            if (!result.getAlternativesList().isEmpty()) {
                sb.append(result.getAlternatives(0).getTranscript()).append(" ");
            }
        }

        var transcript = sb.toString().trim();
        if (!StringUtils.hasText(transcript)) {
            throw new RuntimeException(
                    "No speech detected in video for document " + documentId);
        }
        log.info("Transcription completed for document {}: {} characters",
                documentId, transcript.length());
        return transcript;
    }

    private void deleteTempFile(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temp file {}: {}", path, e.getMessage());
        }
    }

    private void deleteFromGcs(String gcsKey) {
        if (!StringUtils.hasText(gcsKey)) return;
        try {
            storage.delete(BlobId.of(googleSpeechProperties.getGcsBucketName(), gcsKey));
            log.debug("Deleted GCS audio object: {}", gcsKey);
        } catch (Exception e) {
            log.warn("Failed to delete GCS audio object {}: {}", gcsKey, e.getMessage());
        }
    }
}
