package com.reserly.platform.venues.image;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.stereotype.Component;

/**
 * Decodifica y vuelve a codificar JPEG/PNG para validar contenido y eliminar metadatos.
 *
 * <p>No confía en extensión, nombre ni magic bytes aislados. Comprueba dimensiones antes de
 * materializar el raster para limitar bombas de descompresión.
 */
@Component
public class VenueImageContentValidator {

  private final VenueImageUploadProperties properties;

  public VenueImageContentValidator(VenueImageUploadProperties properties) {
    this.properties = properties;
  }

  public ValidatedVenueImage validate(String declaredMediaType, InputStream input) {
    if (declaredMediaType == null || input == null) {
      throw new VenueImageValidationException();
    }
    byte[] source = readBounded(input);
    try (MemoryCacheImageInputStream imageInput =
        new MemoryCacheImageInputStream(new ByteArrayInputStream(source))) {
      ImageReader reader = requireReader(imageInput);
      try {
        reader.setInput(imageInput, false, true);
        String mediaType = mediaType(reader.getFormatName());
        if (!mediaType.equals(declaredMediaType.strip().toLowerCase(Locale.ROOT))) {
          throw new VenueImageValidationException();
        }
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        validateDimensions(width, height);
        if (reader.getNumImages(true) != 1) {
          throw new VenueImageValidationException();
        }
        BufferedImage decoded = reader.read(0);
        byte[] normalized = encode(decoded, mediaType);
        if (normalized.length > properties.maxBytes()) {
          throw new VenueImageValidationException();
        }
        return new ValidatedVenueImage(
            normalized, mediaType, mediaType.equals("image/png") ? "png" : "jpg", width, height);
      } finally {
        reader.dispose();
      }
    } catch (IOException | RuntimeException exception) {
      if (exception instanceof VenueImageValidationException validationException) {
        throw validationException;
      }
      throw new VenueImageValidationException();
    }
  }

  private byte[] readBounded(InputStream input) {
    try (input) {
      byte[] content = input.readNBytes(Math.toIntExact(properties.maxBytes() + 1));
      if (content.length == 0 || content.length > properties.maxBytes()) {
        throw new VenueImageValidationException();
      }
      return content;
    } catch (IOException | ArithmeticException exception) {
      throw new VenueImageValidationException();
    }
  }

  private ImageReader requireReader(MemoryCacheImageInputStream input) {
    Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
    if (!readers.hasNext()) {
      throw new VenueImageValidationException();
    }
    return readers.next();
  }

  private String mediaType(String formatName) {
    return switch (formatName.toLowerCase(Locale.ROOT)) {
      case "jpeg", "jpg" -> "image/jpeg";
      case "png" -> "image/png";
      default -> throw new VenueImageValidationException();
    };
  }

  private void validateDimensions(int width, int height) {
    if (width < properties.minDimension()
        || height < properties.minDimension()
        || width > properties.maxDimension()
        || height > properties.maxDimension()
        || (long) width * height > properties.maxPixels()) {
      throw new VenueImageValidationException();
    }
  }

  private byte[] encode(BufferedImage source, String mediaType) throws IOException {
    BufferedImage output = source;
    if (mediaType.equals("image/jpeg")) {
      output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = output.createGraphics();
      try {
        graphics.drawImage(source, 0, 0, null);
      } finally {
        graphics.dispose();
      }
    }
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    if (!ImageIO.write(output, mediaType.equals("image/png") ? "png" : "jpg", bytes)) {
      throw new VenueImageValidationException();
    }
    return bytes.toByteArray();
  }
}
