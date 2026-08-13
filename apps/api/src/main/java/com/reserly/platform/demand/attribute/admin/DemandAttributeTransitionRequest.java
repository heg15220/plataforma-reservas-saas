package com.reserly.platform.demand.attribute.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Decisión humana; las fusiones requieren destino y las decisiones terminales un motivo. */
public record DemandAttributeTransitionRequest(
    @NotBlank @Pattern(regexp = "in_review|published|merged|retired|rejected") String status,
    UUID targetAttributeId,
    @Size(max = 1000) String reason) {}
