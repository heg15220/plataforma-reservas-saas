package com.reserly.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** Protege la zona explícita usada por asistencia, cancelación y auditoría. */
class BusinessClockConfigurationTests {

  private final BusinessClockConfiguration configuration =
      new BusinessClockConfiguration();

  @Test
  void createsClockForConfiguredIanaZone() {
    assertThat(configuration.businessClock("Europe/Madrid").getZone())
        .isEqualTo(ZoneId.of("Europe/Madrid"));
  }

  @Test
  void rejectsInvalidZoneAtStartup() {
    assertThatThrownBy(() -> configuration.businessClock("Not/AZone"))
        .isInstanceOf(IllegalStateException.class);
  }
}
