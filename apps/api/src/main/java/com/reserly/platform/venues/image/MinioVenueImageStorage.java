package com.reserly.platform.venues.image;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import org.springframework.stereotype.Component;

/** Adaptador MinIO que nunca concede acceso anónimo al bucket de imágenes. */
@Component
public class MinioVenueImageStorage implements VenueImageStorage {

  private final VenueImageStorageProperties properties;
  private final MinioClient client;

  public MinioVenueImageStorage(VenueImageStorageProperties properties) {
    this.properties = properties;
    this.client =
        MinioClient.builder()
            .endpoint(properties.endpoint().toString())
            .credentials(properties.accessKey(), properties.secretKey())
            .region(properties.region())
            .build();
  }

  @Override
  public void put(String objectKey, byte[] content, String mediaType) {
    try {
      ensureBucket();
      client.putObject(
          PutObjectArgs.builder()
              .bucket(properties.bucket())
              .object(objectKey)
              .contentType(mediaType)
              .stream(new ByteArrayInputStream(content), content.length, -1)
              .build());
    } catch (Exception exception) {
      throw new VenueImageStorageException();
    }
  }

  @Override
  public byte[] get(String objectKey) {
    try (var content =
        client.getObject(
            GetObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build())) {
      return content.readAllBytes();
    } catch (Exception exception) {
      throw new VenueImageStorageException();
    }
  }

  @Override
  public void delete(String objectKey) {
    try {
      client.removeObject(
          RemoveObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build());
    } catch (Exception exception) {
      throw new VenueImageStorageException();
    }
  }

  private void ensureBucket() throws Exception {
    if (client.bucketExists(BucketExistsArgs.builder().bucket(properties.bucket()).build())) {
      return;
    }
    if (!properties.createBucket()) {
      throw new VenueImageStorageException();
    }
    client.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
  }
}
