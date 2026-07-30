package com.reserly.platform.administration.dto;

import java.util.List;

/** Cola acotada de identidades empresariales pendientes de decisión manual. */
public record AdminBusinessAccountListResponse(List<AdminBusinessAccountResponse> accounts) {}
