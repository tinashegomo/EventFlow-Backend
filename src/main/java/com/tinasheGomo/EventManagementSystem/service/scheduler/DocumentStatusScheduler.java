package com.tinasheGomo.EventManagementSystem.service.scheduler;

import com.tinasheGomo.EventManagementSystem.service.invoice.InvoiceService;
import com.tinasheGomo.EventManagementSystem.service.quotation.QuotationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the date-driven document statuses (invoice OVERDUE, quotation EXPIRED) correct on
 * the server instead of relying on a browser opening the right page at the right time.
 * Runs shortly after midnight, when those date comparisons change.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentStatusScheduler {

    private final InvoiceService invoiceService;
    private final QuotationService quotationService;

    @Scheduled(cron = "${app.scheduler.document-status-cron:0 5 0 * * *}")
    public void refreshDocumentStatuses() {
        int overdue = invoiceService.markOverdueInvoices();
        int expired = quotationService.markExpiredQuotations();
        if (overdue > 0 || expired > 0) {
            log.info("Document status refresh: {} invoice(s) marked OVERDUE, {} quotation(s) marked EXPIRED",
                    overdue, expired);
        }
    }
}
