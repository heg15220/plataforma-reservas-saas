package com.reserly.platform.administration.dto;

import java.util.List;

/** Listado acotado de penalizaciones para gestión administrativa. */
public record AdminPenaltyListResponse(List<AdminPenaltyResponse> penalties) {}
