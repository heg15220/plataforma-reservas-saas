package com.reserly.platform.development;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Protege el contrato mínimo de los datos empaquetados para el recorrido local anónimo. */
class LocalDemoVenueFixtureContractTests {

  @Test
  void scriptPublishesSixStableVenuesAcrossDistinctCategoriesAndRollingCapacity() throws Exception {
    String sql =
        new ClassPathResource("dev-fixtures/local-demo-venues.sql")
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(sql)
        .contains("'ames-padel-center'")
        .contains("'lume-de-bretema'")
        .contains("'brisa-studio'")
        .contains("'campo-do-sar'")
        .contains("'norte-fitness-lab'")
        .contains("'aura-atlantica'")
        .contains("INSERT INTO \"VenueCustomTabs\"")
        .contains("Tarifas y alquiler")
        .contains("Estilos y precios")
        .contains("Carta y precios")
        .contains("Modalidades y tarifas")
        .contains("Entrenamientos y cuotas")
        .contains("Tratamientos y precios")
        .contains("'20000000-0000-0000-0000-000000000002'")
        .contains("'20000000-0000-0000-0000-000000000003'")
        .contains("'20000000-0000-0000-0000-000000000006'")
        .contains("'20000000-0000-0000-0000-000000000007'")
        .contains("'20000000-0000-0000-0000-000000000001'")
        .contains("'Reserva de mesa'")
        .contains("TIME '13:00', TIME '14:30'")
        .contains("TIME '21:00', TIME '22:30'")
        .contains("  18,")
        .contains("reservationFormPublished")
        .contains("businessVerificationExpiresAt")
        .contains("CURRENT_DATE + 30")
        .contains("ON CONFLICT DO NOTHING")
        .contains("'available'")
        .contains("Mailpit");

    assertThat(sql)
        .contains("multilocal@reserly.local")
        .contains("ReserlyLocal2026!")
        .contains("\"notificationEmail\"")
        .contains("La cuenta multilocal autenticable gestiona Ames Padel Center y Brisa Studio");

    assertThat(sql.substring(sql.indexOf("-- Sustituye el fixture histórico")))
        .doesNotContain("'let-padel-ames'");
  }

  @Test
  void packagedImagesKeepTheMetadataPersistedByTheFixture() throws Exception {
    List<ExpectedImage> images =
        List.of(
            new ExpectedImage("dev-fixtures/images/ames-padel-center.png", 907, 808),
            new ExpectedImage("dev-fixtures/images/padel-courts.jpg", 1360, 1016),
            new ExpectedImage("dev-fixtures/images/brisa-studio-main.jpg", 440, 440),
            new ExpectedImage("dev-fixtures/images/brisa-studio-gallery.jpg", 440, 443),
            new ExpectedImage("dev-fixtures/images/campo-do-sar-main.jpg", 440, 440),
            new ExpectedImage("dev-fixtures/images/campo-do-sar-gallery.jpg", 440, 443),
            new ExpectedImage("dev-fixtures/images/norte-fitness-lab-main.jpg", 440, 440),
            new ExpectedImage("dev-fixtures/images/norte-fitness-lab-gallery.jpg", 440, 443),
            new ExpectedImage("dev-fixtures/images/aura-atlantica-main.jpg", 444, 440),
            new ExpectedImage("dev-fixtures/images/aura-atlantica-gallery.jpg", 444, 443),
            new ExpectedImage("dev-fixtures/images/lume-de-bretema-main.png", 1536, 1024),
            new ExpectedImage("dev-fixtures/images/lume-de-bretema-dish.png", 1536, 1024),
            new ExpectedImage("dev-fixtures/images/lume-de-bretema-kitchen.png", 1536, 1024));

    for (ExpectedImage expected : images) {
      ClassPathResource resource = new ClassPathResource(expected.path());
      assertThat(resource.exists()).isTrue();
      try (var input = resource.getInputStream()) {
        BufferedImage image = ImageIO.read(input);
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(expected.width());
        assertThat(image.getHeight()).isEqualTo(expected.height());
      }
    }
  }

  private record ExpectedImage(String path, int width, int height) {}
}
