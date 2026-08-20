-- Vincula la intención de lista de espera al servicio exacto que el hold ordinario debe revalidar.
ALTER TABLE "WaitlistEntries" ADD COLUMN "serviceId" uuid;
ALTER TABLE "WaitlistEntries" ADD CONSTRAINT "fkWaitlistEntriesService"
  FOREIGN KEY ("serviceId") REFERENCES "Services" ("id") ON DELETE RESTRICT;

CREATE INDEX "ixWaitlistEntriesServiceQueue"
  ON "WaitlistEntries" ("serviceId", "status", "createdAt");

COMMENT ON COLUMN "WaitlistEntries"."serviceId" IS
  'Servicio solicitado e inmutable que ReservationHoldService contrasta con la franja al aceptar';
