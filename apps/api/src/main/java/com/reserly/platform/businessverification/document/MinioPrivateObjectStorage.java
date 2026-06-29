package com.reserly.platform.businessverification.document;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import org.springframework.stereotype.Component;

/**
 * Adaptador S3-compatible basado en el cliente MinIO.
 *
 * <p>No aplica políticas públicas ni devuelve URLs. El bucket solo se crea automáticamente cuando
 * la configuración local lo permite.
 */
@Component
public class MinioPrivateObjectStorage implements PrivateObjectStorage {

  private static final String ENCRYPTED_MEDIA_TYPE = "application/octet-stream";

  private final PrivateObjectStorageProperties properties;
  private final MinioClient client;

  public MinioPrivateObjectStorage(PrivateObjectStorageProperties properties) {
    this.properties = properties;
    this.client =
        MinioClient.builder()
            .endpoint(properties.endpoint().toString())
            .credentials(properties.accessKey(), properties.secretKey())
            .region(properties.region())
            .build();
  }

  @Override
  public void put(String objectKey, byte[] encryptedContent) {
    try {
      ensureBucket();
      client.putObject(
          PutObjectArgs.builder()
              .bucket(properties.bucket())
              .object(objectKey)
              .contentType(ENCRYPTED_MEDIA_TYPE)
              .stream(new ByteArrayInputStream(encryptedContent), encryptedContent.length, -1)
              .build());
    } catch (Exception exception) {
      throw new PrivateDocumentStorageException();
    }
  }

  @Override
  public void delete(String objectKey) {
    try {
      client.removeObject(
          RemoveObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build());
    } catch (Exception exception) {
      throw new PrivateDocumentStorageException();
    }
  }

  private void ensureBucket() throws Exception {
    boolean exists =
        client.bucketExists(BucketExistsArgs.builder().bucket(properties.bucket()).build());
    if (exists) {
      return;
    }
    if (!properties.createBucket()) {
      throw new PrivateDocumentStorageException();
    }
    client.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
  }
}
