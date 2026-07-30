package com.reserly.platform.administration.dto;

import java.util.List;

/** Catálogo administrativo de planes. */
public record AdminPlanListResponse(List<AdminPlanResponse> plans) {}
