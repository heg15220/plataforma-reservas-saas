-- Endurece AuditLogs como ledger append-only y hace idempotentes los eventos de gobierno de demanda.
-- Las siete familias usan entityId como eventId técnico; nunca se guarda un sujeto o dato personal.

CREATE OR REPLACE FUNCTION prevent_audit_log_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  RAISE EXCEPTION 'AuditLogs is append-only';
END;
$$;

CREATE TRIGGER "trgAuditLogsAppendOnly"
BEFORE UPDATE OR DELETE ON "AuditLogs"
FOR EACH ROW
EXECUTE FUNCTION prevent_audit_log_mutation();

CREATE UNIQUE INDEX "uxAuditLogsDemandGovernanceEvent"
  ON "AuditLogs" ("entityType", "entityId")
  WHERE "entityType" IN (
    'demand_ontology',
    'demand_ranking_weights',
    'demand_model',
    'demand_experiment',
    'demand_promotion',
    'demand_waitlist',
    'demand_automatic_action'
  );

COMMENT ON FUNCTION prevent_audit_log_mutation() IS
  'Impide sobrescribir o borrar evidencia administrativa; correcciones generan una nueva entrada';
COMMENT ON INDEX "uxAuditLogsDemandGovernanceEvent" IS
  'Garantiza idempotencia por familia y eventId para reintentos autenticados del plano MLOps';
