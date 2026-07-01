package com.reserly.platform.venues.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Verifica MIME real, límites dimensionales y normalización de imágenes. */
class VenueImageContentValidatorTests {

  private final VenueImageContentValidator validator =
      new VenueImageContentValidator(
          new VenueImageUploadProperties(5_242_880, 320, 4096, 16_777_216));

  @Test
  void acceptsAndReencodesAValidPng() throws IOException {
    byte[] source = image("png", 320, 400);

    ValidatedVenueImage result = validator.validate("image/png", new ByteArrayInputStream(source));

    assertThat(result.mediaType()).isEqualTo("image/png");
    assertThat(result.extension()).isEqualTo("png");
    assertThat(result.width()).isEqualTo(320);
    assertThat(result.height()).isEqualTo(400);
    assertThat(ImageIO.read(new ByteArrayInputStream(result.bytes()))).isNotNull();
  }

  @Test
  void rejectsMimeSpoofingAndUnknownContent() throws IOException {
    byte[] jpeg = image("jpg", 320, 320);

    assertThatThrownBy(() -> validator.validate("image/png", new ByteArrayInputStream(jpeg)))
        .isInstanceOf(VenueImageValidationException.class);
    assertThatThrownBy(
            () -> validator.validate("image/png", new ByteArrayInputStream("not-image".getBytes())))
        .isInstanceOf(VenueImageValidationException.class);
  }

  @Test
  void rejectsDimensionsOutsideTheSafeEnvelope() throws IOException {
    byte[] tooSmall = image("png", 319, 320);
    byte[] tooWide = image("png", 4097, 320);

    assertThatThrownBy(() -> validator.validate("image/png", new ByteArrayInputStream(tooSmall)))
        .isInstanceOf(VenueImageValidationException.class);
    assertThatThrownBy(() -> validator.validate("image/png", new ByteArrayInputStream(tooWide)))
        .isInstanceOf(VenueImageValidationException.class);
  }

  private byte[] image(String format, int width, int height) throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    var graphics = image.createGraphics();
    graphics.setColor(Color.BLUE);
    graphics.fillRect(0, 0, width, height);
    graphics.dispose();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, format, output);
    return output.toByteArray();
  }
}
