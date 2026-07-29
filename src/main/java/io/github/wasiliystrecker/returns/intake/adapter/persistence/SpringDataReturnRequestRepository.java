package io.github.wasiliystrecker.returns.intake.adapter.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataReturnRequestRepository extends JpaRepository<ReturnRequestEntity, UUID> {

  boolean existsByOrderReferenceAndItemReference(String orderReference, String itemReference);
}
