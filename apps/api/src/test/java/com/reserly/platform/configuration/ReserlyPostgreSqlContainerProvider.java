package com.reserly.platform.configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainerProvider;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.jdbc.ConnectionUrl;
import org.testcontainers.utility.DockerImageName;

/**
 * Proveedor JDBC Testcontainers para la imagen PostgreSQL compartida por Reserly.
 *
 * <p>Construye el Dockerfile versionado del repositorio antes de iniciar el contenedor. Con ello,
 * las migraciones se prueban con las mismas versiones de PostgreSQL, PostGIS y pgvector que el
 * entorno local, sin depender de una imagen privada previamente publicada.
 */
public final class ReserlyPostgreSqlContainerProvider extends JdbcDatabaseContainerProvider {

  private static final String DATABASE_TYPE = "reserly";
  private static final String IMAGE_REPOSITORY = "reserly/postgres";
  private static final String SUPPORTED_TAG = "17-3.5-vector0.8.6";

  @Override
  public boolean supports(String databaseType) {
    return DATABASE_TYPE.equals(databaseType);
  }

  @Override
  public JdbcDatabaseContainer<?> newInstance() {
    return newInstance(SUPPORTED_TAG);
  }

  @Override
  public JdbcDatabaseContainer<?> newInstance(String tag) {
    if (!SUPPORTED_TAG.equals(tag)) {
      throw new IllegalArgumentException(
          "Versión de PostgreSQL Reserly no soportada por los tests: " + tag);
    }

    String imageName = IMAGE_REPOSITORY + ":" + tag;
    new ImageFromDockerfile(imageName, false).withDockerfile(findDockerfile()).get();

    DockerImageName compatibleImage =
        DockerImageName.parse(imageName).asCompatibleSubstituteFor("postgres");
    return new PostgreSQLContainer<>(compatibleImage);
  }

  @Override
  public JdbcDatabaseContainer<?> newInstance(ConnectionUrl connectionUrl) {
    return newInstanceFromConnectionUrl(connectionUrl, "user", "password");
  }

  /**
   * Localiza la raíz del repositorio tanto al lanzar Maven desde la raíz como desde {@code
   * apps/api}. Se limita el ascenso para que un checkout incompleto falle de forma explicita antes
   * de Flyway.
   */
  private Path findDockerfile() {
    Path directory = Path.of("").toAbsolutePath().normalize();
    for (int depth = 0; directory != null && depth < 6; depth++) {
      Path candidate = directory.resolve("infrastructure/postgres/Dockerfile");
      if (Files.isRegularFile(candidate)) {
        return candidate;
      }
      directory = directory.getParent();
    }
    throw new IllegalStateException(
        "No se encontró infrastructure/postgres/Dockerfile desde el directorio de trabajo");
  }
}
