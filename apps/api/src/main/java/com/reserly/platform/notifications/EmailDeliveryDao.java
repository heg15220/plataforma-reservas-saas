package com.reserly.platform.notifications;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** DAO del registro idempotente de entrega por evento y clase de destinatario. */
public interface EmailDeliveryDao extends JpaRepository<EmailDeliveryEntity, UUID> {
  Optional<EmailDeliveryEntity> findByEventIdAndRecipientKind(UUID eventId, String recipientKind);
}
