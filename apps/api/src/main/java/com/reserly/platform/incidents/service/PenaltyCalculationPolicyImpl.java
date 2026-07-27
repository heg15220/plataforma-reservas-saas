package com.reserly.platform.incidents.service;

import java.time.Duration;
import org.springframework.stereotype.Component;

/** Implementa el escalado global definido por RB-007 sin depender de persistencia o reloj. */
@Component
public class PenaltyCalculationPolicyImpl implements PenaltyCalculationPolicy {

  @Override
  public Duration durationFor(int incidentCountOperational) {
    if (incidentCountOperational < 1) {
      throw new IllegalArgumentException("Incident count must be positive");
    }
    int days =
        switch (incidentCountOperational) {
          case 1 -> 7;
          case 2 -> 14;
          case 3 -> 21;
          default -> 60;
        };
    return Duration.ofDays(days);
  }
}
