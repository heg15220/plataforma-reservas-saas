-- Cola humana para atributos y decisiones comerciales materiales, con corrección e impugnación.
CREATE TABLE "DemandGovernanceReviews" (
  "id" uuid PRIMARY KEY,
  "reviewType" varchar(32) NOT NULL,
  "subjectType" varchar(48) NOT NULL,
  "subjectKey" varchar(128) NOT NULL,
  "subjectVersion" varchar(64) NOT NULL,
  "venueId" uuid,
  "policyVersion" varchar(64) NOT NULL,
  "explanationCode" varchar(64) NOT NULL,
  "evidenceSha256" char(64) NOT NULL,
  "status" varchar(32) NOT NULL DEFAULT 'submitted',
  "requestedByService" varchar(64) NOT NULL,
  "reviewerUserId" uuid,
  "reviewReasonCode" varchar(64),
  "correctionVersion" varchar(64),
  "appealCode" varchar(64),
  "appealedByUserId" uuid,
  "submittedAt" timestamp with time zone NOT NULL,
  "reviewedAt" timestamp with time zone,
  "appealedAt" timestamp with time zone,
  "updatedAt" timestamp with time zone NOT NULL,
  "version" integer NOT NULL DEFAULT 0,
  CONSTRAINT "fkDemandGovernanceReviewsVenue" FOREIGN KEY ("venueId") REFERENCES "Venues" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fkDemandGovernanceReviewsReviewer" FOREIGN KEY ("reviewerUserId") REFERENCES "Users" ("id") ON DELETE SET NULL,
  CONSTRAINT "fkDemandGovernanceReviewsAppellant" FOREIGN KEY ("appealedByUserId") REFERENCES "Users" ("id") ON DELETE SET NULL,
  CONSTRAINT "ckDemandGovernanceReviewsType" CHECK ("reviewType" IN ('attribute', 'commercial_decision')),
  CONSTRAINT "ckDemandGovernanceReviewsTokens" CHECK (
    "subjectType" ~ '^[a-z][a-z0-9._-]{0,47}$'
    AND "subjectKey" ~ '^[a-z][A-Za-z0-9._:-]{0,127}$'
    AND "subjectVersion" ~ '^[a-z0-9][A-Za-z0-9._-]{0,63}$'
    AND "policyVersion" ~ '^[a-z][A-Za-z0-9._-]{0,63}$'
    AND "explanationCode" ~ '^[a-z][a-z0-9._-]{0,63}$'
    AND "evidenceSha256" ~ '^[a-f0-9]{64}$'
  ),
  CONSTRAINT "ckDemandGovernanceReviewsStatus" CHECK (
    "status" IN ('submitted', 'approved', 'rejected', 'correction_requested', 'corrected', 'appealed')
  ),
  CONSTRAINT "ckDemandGovernanceReviewsScope" CHECK (
    ("reviewType" = 'attribute' AND "venueId" IS NULL)
    OR ("reviewType" = 'commercial_decision' AND "venueId" IS NOT NULL)
  ),
  CONSTRAINT "ckDemandGovernanceReviewsTimeline" CHECK (
    "updatedAt" >= "submittedAt"
    AND ("reviewedAt" IS NULL OR "reviewedAt" >= "submittedAt")
    AND ("appealedAt" IS NULL OR "appealedAt" >= "submittedAt")
  )
);

CREATE INDEX "ixDemandGovernanceReviewsQueue" ON "DemandGovernanceReviews" ("status", "updatedAt" DESC);
CREATE INDEX "ixDemandGovernanceReviewsVenue" ON "DemandGovernanceReviews" ("venueId", "updatedAt" DESC) WHERE "venueId" IS NOT NULL;

COMMENT ON TABLE "DemandGovernanceReviews" IS 'Revisión humana previa, corrección e impugnación de decisiones materiales del motor de demanda';
