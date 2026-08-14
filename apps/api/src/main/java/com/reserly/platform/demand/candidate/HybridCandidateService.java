package com.reserly.platform.demand.candidate;

import java.util.List;

/** Puerto interno de recuperación; no publica ni confirma reservas. */
public interface HybridCandidateService {
  List<HybridCandidate> generate(HybridCandidateQuery query);
}
