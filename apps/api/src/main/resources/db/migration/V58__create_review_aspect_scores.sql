-- Conserva únicamente derivados ABSA; el comentario sigue en Reviews y no se duplica.
CREATE TABLE "ReviewAspectScores" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "reviewId" uuid NOT NULL,
  "venueId" uuid NOT NULL,
  "demandAttributeId" uuid NOT NULL,
  "score" numeric(9, 8) NOT NULL,
  "confidence" numeric(9, 8) NOT NULL,
  "evidenceCount" integer NOT NULL,
  "extractorVersion" varchar(64) NOT NULL,
  "policyVersion" varchar(64) NOT NULL,
  "reviewStatus" varchar(24) NOT NULL,
  "humanScore" numeric(9, 8),
  "humanReviewedAt" timestamp with time zone,
  "observedAt" timestamp with time zone NOT NULL,
  "expiresAt" timestamp with time zone NOT NULL,
  "createdAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uqReviewAspectScoresReviewAttribute" UNIQUE ("reviewId", "demandAttributeId"),
  CONSTRAINT "fkReviewAspectScoresReview" FOREIGN KEY ("reviewId")
    REFERENCES "Reviews" ("id") ON DELETE CASCADE,
  CONSTRAINT "fkReviewAspectScoresVenue" FOREIGN KEY ("venueId")
    REFERENCES "Venues" ("id") ON DELETE CASCADE,
  CONSTRAINT "fkReviewAspectScoresAttribute" FOREIGN KEY ("demandAttributeId")
    REFERENCES "DemandAttributes" ("id") ON DELETE RESTRICT,
  CONSTRAINT "ckReviewAspectScoresValues" CHECK (
    "score" BETWEEN -1 AND 1 AND "confidence" BETWEEN 0 AND 1
    AND "evidenceCount" BETWEEN 1 AND 50
    AND ("humanScore" IS NULL OR "humanScore" BETWEEN -1 AND 1)
  ),
  CONSTRAINT "ckReviewAspectScoresVersions" CHECK (
    "extractorVersion" ~ '^[a-z][A-Za-z0-9._-]{0,63}$'
    AND "policyVersion" ~ '^[a-z][A-Za-z0-9._-]{0,63}$'
  ),
  CONSTRAINT "ckReviewAspectScoresReview" CHECK (
    "reviewStatus" IN ('machineAccepted', 'pendingHuman', 'humanAccepted', 'humanCorrected', 'rejected')
    AND (("humanScore" IS NULL AND "humanReviewedAt" IS NULL)
      OR ("humanScore" IS NOT NULL AND "humanReviewedAt" IS NOT NULL))
    AND ("reviewStatus" NOT IN ('humanAccepted', 'humanCorrected') OR "humanScore" IS NOT NULL)
  ),
  CONSTRAINT "ckReviewAspectScoresTimes" CHECK (
    "observedAt" <= "createdAt" AND "expiresAt" > "observedAt"
    AND "updatedAt" >= "createdAt"
    AND ("humanReviewedAt" IS NULL OR "humanReviewedAt" >= "observedAt")
  )
);

CREATE INDEX "ixReviewAspectScoresVenueAttributeExpiry"
  ON "ReviewAspectScores" ("venueId", "demandAttributeId", "expiresAt", "reviewStatus");
CREATE INDEX "ixReviewAspectScoresHumanQueue"
  ON "ReviewAspectScores" ("reviewStatus", "createdAt")
  WHERE "reviewStatus" = 'pendingHuman';

COMMENT ON TABLE "ReviewAspectScores" IS
  'Derivados ABSA versionados de reseñas verificadas, sin copia del comentario o identidad';
