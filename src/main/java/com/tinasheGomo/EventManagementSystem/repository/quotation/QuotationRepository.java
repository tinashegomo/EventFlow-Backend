package com.tinasheGomo.EventManagementSystem.repository.quotation;

import com.tinasheGomo.EventManagementSystem.entity.organization.OrganizationEntity;
import com.tinasheGomo.EventManagementSystem.entity.quotation.QuotationEntity;
import com.tinasheGomo.EventManagementSystem.enums.QuotationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface QuotationRepository extends JpaRepository<QuotationEntity, String> {
    List<QuotationEntity> findByOrganizationOrderByCreatedAtDesc(OrganizationEntity organization);

    /** Cross-organisation lookup used by the status scheduler (no authenticated user). */
    List<QuotationEntity> findByStatusInAndValidUntilBefore(List<QuotationStatus> statuses, LocalDate date);
}
