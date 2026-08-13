package com.reserly.platform.demand.attribute.admin;

import java.util.List;

/** Snapshot conjunto necesario para operar el panel de gobierno. */
public record DemandAttributeAdminListResponse(
    List<DemandAttributeAdminResponse> attributes,
    List<DemandAttributeCandidateAdminResponse> candidates) {}
