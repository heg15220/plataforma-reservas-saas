package com.reserly.platform.development;

import com.reserly.platform.venues.image.VenueImageStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * Instala publicaciones de demostración únicamente en el perfil local.
 *
 * <p>Los objetos de imagen se escriben con claves estables y el script SQL usa identificadores
 * reservados y operaciones idempotentes. Reiniciar la API repone los datos base y amplía la
 * disponibilidad móvil sin crear propietarios o publicaciones duplicadas.
 */
@Component
@Profile("local")
@ConditionalOnProperty(
    prefix = "reserly.development",
    name = "demo-venues-enabled",
    havingValue = "true",
    matchIfMissing = true)
public class LocalDemoVenueInitializer implements ApplicationRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalDemoVenueInitializer.class);
  private static final String SQL_RESOURCE = "dev-fixtures/local-demo-venues.sql";
  private static final List<DemoImage> IMAGES =
      List.of(
          new DemoImage(
              "dev-fixtures/images/ames-padel-center.png",
              "dev-fixtures/venues/ames-padel-center/main.png",
              "image/png"),
          new DemoImage(
              "dev-fixtures/images/padel-courts.jpg",
              "dev-fixtures/venues/ames-padel-center/gallery-1.jpg",
              "image/jpeg"),
          new DemoImage(
              "dev-fixtures/images/let-padel-ames.jpg",
              "dev-fixtures/venues/let-padel-ames/main.jpg",
              "image/jpeg"),
          new DemoImage(
              "dev-fixtures/images/padel-courts.jpg",
              "dev-fixtures/venues/let-padel-ames/gallery-1.jpg",
              "image/jpeg"),
          new DemoImage(
              "dev-fixtures/images/lume-de-bretema-main.png",
              "dev-fixtures/venues/lume-de-bretema/main.png",
              "image/png"),
          new DemoImage(
              "dev-fixtures/images/lume-de-bretema-dish.png",
              "dev-fixtures/venues/lume-de-bretema/gallery-1.png",
              "image/png"),
          new DemoImage(
              "dev-fixtures/images/lume-de-bretema-kitchen.png",
              "dev-fixtures/venues/lume-de-bretema/gallery-2.png",
              "image/png"));

  private final javax.sql.DataSource dataSource;
  private final VenueImageStorage imageStorage;

  public LocalDemoVenueInitializer(
      javax.sql.DataSource dataSource, VenueImageStorage imageStorage) {
    this.dataSource = dataSource;
    this.imageStorage = imageStorage;
  }

  /**
   * Sube primero las imágenes para evitar publicar referencias rotas y después aplica el seed SQL.
   *
   * @throws UncheckedIOException si un recurso empaquetado no puede leerse; el arranque se detiene
   *     porque el modo demo solicitado no quedaría verificable
   */
  @Override
  public void run(ApplicationArguments args) {
    IMAGES.forEach(this::store);
    new ResourceDatabasePopulator(new ClassPathResource(SQL_RESOURCE)).execute(dataSource);
    LOGGER.info(
        "Fixtures locales preparados: /locales/ames-padel-center, /locales/let-padel-ames y "
            + "/locales/lume-de-bretema");
  }

  private void store(DemoImage image) {
    try {
      byte[] content = new ClassPathResource(image.classpathLocation()).getInputStream().readAllBytes();
      imageStorage.put(image.objectKey(), content, image.mediaType());
    } catch (IOException exception) {
      throw new UncheckedIOException(
          "No se pudo leer la imagen local " + image.classpathLocation(), exception);
    }
  }

  private record DemoImage(String classpathLocation, String objectKey, String mediaType) {}
}
