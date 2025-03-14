/*
 * MIT License
 *
 * Copyright (c) 2024 William278
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.william278.backend.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.model.Asset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Slf4j
@Service
public class S3Service {

    private final boolean enabled;

    private MinioClient client;
    private AppConfiguration config;

    @Autowired
    public S3Service(@NotNull AppConfiguration config) {
        this.enabled = config.getS3AccessKey() != null && config.getS3SecretKey() != null
                && !config.getS3AccessKey().isEmpty() && !config.getS3SecretKey().isEmpty();
        if (!enabled) {
            return;
        }

        this.config = config;
        this.client = MinioClient.builder()
                .endpoint(config.getS3Endpoint())
                .credentials(config.getS3AccessKey(), config.getS3SecretKey())
                .build();
    }

    public Optional<String> getTranscriptUrl(long ticketNumber) {
        if (!enabled) {
            log.info("S3 disabled, Skipping getting transcript URL for ticket #{}", ticketNumber);
            return Optional.empty();
        }

        try {
            return Optional.of(client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .expiry(Integer.parseInt(config.getS3TicketsExpiry()))
                    .bucket(config.getS3TicketsBucket())
                    .object(getTicketObjectName(ticketNumber))
                    .method(Method.GET)
                    .build()));
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | XmlParserException |
                 ServerException e) {
            log.warn("Failed to fetch ticket transcript #{}", ticketNumber, e);
        }
        return Optional.empty();
    }

    public void deleteTranscript(long ticketNumber) {
        if (!enabled) {
            log.info("S3 disabled, Skipping deleting transcript #{}", ticketNumber);
            return;
        }

        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(config.getS3TicketsBucket())
                    .object(getTicketObjectName(ticketNumber))
                    .build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | XmlParserException |
                 ServerException e) {
            log.warn("Failed to delete ticket transcript #{}", ticketNumber, e);
        }
    }

    public void uploadAsset(@NotNull MultipartFile file, @NotNull Asset asset) {
        if (!enabled) {
            log.info("S3 disabled, skipping asset upload for {}", asset.getName());
            return;
        }

        try {
            client.putObject(PutObjectArgs.builder()
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .bucket(config.getS3AssetsBucket())
                    .object(asset.getName())
                    .contentType(file.getContentType())
                    .build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | XmlParserException |
                 ServerException e) {
            log.warn("Failed to create or upload asset '{}'", asset.getName(), e);
        }
    }

    public void deleteAsset(@NotNull Asset asset) {
        if (!enabled) {
            log.info("S3 disabled, skipping asset deletion for {}", asset.getName());
            return;
        }

        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .object(asset.getName())
                    .bucket(config.getS3AssetsBucket())
                    .build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | XmlParserException |
                 ServerException e) {
            log.warn("Failed to delete asset '{}'", asset.getName(), e);
        }
    }

    public void uploadVersion(@NotNull InputStream versionStream, long size,
                              @NotNull String contentType, @NotNull String objectName) {
        if (!enabled) {
            log.info("S3 disabled, skipping version upload for {}", objectName);
            return;
        }

        try {
            client.putObject(PutObjectArgs.builder()
                    .stream(versionStream, size, -1)
                    .bucket(config.getS3DownloadsBucket())
                    .object(objectName)
                    .contentType(contentType)
                    .build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | XmlParserException |
                 ServerException e) {
            log.warn("Failed to upload version '{}'", objectName, e);
        }
    }

    @Nullable
    public InputStream downloadVersion(@NotNull String objectName) {
        if (!enabled) {
            log.info("S3 disabled, cannot download version {}", objectName);
            return null;
        }

        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(config.getS3DownloadsBucket())
                    .object(objectName)
                    .build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | XmlParserException |
                 ServerException e) {
            log.warn("Failed to download version '{}'", objectName, e);
        }
        return null;
    }

    @NotNull
    private static String getTicketObjectName(long ticketNumber) {
        return String.format("ticket-%04d".formatted(ticketNumber));
    }

}
