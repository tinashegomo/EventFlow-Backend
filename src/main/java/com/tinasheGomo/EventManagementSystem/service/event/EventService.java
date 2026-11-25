package com.tinasheGomo.EventManagementSystem.service.event;

import com.tinasheGomo.EventManagementSystem.dto.event.*;
import com.tinasheGomo.EventManagementSystem.entity.event.AttachedItemEntity;
import com.tinasheGomo.EventManagementSystem.entity.event.EventEntity;
import com.tinasheGomo.EventManagementSystem.entity.event.PaymentEntity;
import com.tinasheGomo.EventManagementSystem.entity.eventtype.EventTypeEntity;
import com.tinasheGomo.EventManagementSystem.entity.eventtype.PricingTierEntity;
import com.tinasheGomo.EventManagementSystem.entity.organization.OrganizationEntity;
import com.tinasheGomo.EventManagementSystem.enums.EventStatus;
import com.tinasheGomo.EventManagementSystem.exception.exceptions.BusinessRuleException;
import com.tinasheGomo.EventManagementSystem.exception.exceptions.NotFoundException;
import com.tinasheGomo.EventManagementSystem.mapper.event.EventMapper;
import com.tinasheGomo.EventManagementSystem.repository.event.EventRepository;
import com.tinasheGomo.EventManagementSystem.repository.eventtype.EventTypeRepository;
import com.tinasheGomo.EventManagementSystem.repository.organization.OrganizationRepository;
import com.tinasheGomo.EventManagementSystem.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventTypeRepository eventTypeRepository;
    private final OrganizationRepository organizationRepository;
    private final EventMapper eventMapper;

    public List<EventResponseDTO> getByOrg(String orgId) {
        SecurityUtils.requireOrgAccess(orgId);
        OrganizationEntity org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organization not found: " + orgId));
        return eventMapper.toResponseList(eventRepository.findByOrganizationOrderByCreatedAtDesc(org));
    }

    public EventResponseDTO getById(String id) {
        EventEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found: " + id));
        SecurityUtils.requireResourceAccess(event.getOrganization(), "event");
        return eventMapper.toResponse(event);
    }

    @Transactional
    public EventResponseDTO create(String orgId, EventRequestDTO request, String userId) {
        SecurityUtils.requireOrgAccess(orgId);
        OrganizationEntity org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organization not found: " + orgId));

        LocalDate scheduledDate = LocalDate.parse(request.getScheduledDate());
        if (scheduledDate.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Scheduled date cannot be in the past");
        }

        EventTypeEntity eventType = eventTypeRepository.findById(request.getEventTypeId())
                .orElseThrow(() -> new NotFoundException("Event type not found: " + request.getEventTypeId()));
        SecurityUtils.requireResourceAccess(eventType.getOrganization(), "event type");

        EventEntity event = new EventEntity();
        event.setTitle(request.getTitle());
        event.setEventType(eventType);
        event.setStatus(request.getStatus() != null
                ? EventStatus.valueOf(request.getStatus()) : EventStatus.SCHEDULED);
        event.setClientName(request.getClientName());
        event.setClientPhone(request.getClientPhone());
        event.setClientEmail(request.getClientEmail());
        event.setVenue(request.getVenue());
        event.setScheduledDate(scheduledDate);
        event.setScheduledTime(request.getScheduledTime());
        event.setGuestCount(request.getGuestCount());
        event.setPriceMode(request.getPriceMode());
        event.setSelectedPricingTierId(request.getSelectedPricingTierId());
        event.setCustomPrice(request.getCustomPrice());
        event.setNotes(request.getNotes());
        event.setCreatedBy(userId);
        event.setOrganization(org);

        event.setTotalPrice(resolveTotalPrice(eventType, request));

        applyAttachedItems(event, request.getAttachedItems());

        if (request.getInitialDeposit() != null && request.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
            PaymentEntity payment = new PaymentEntity();
            payment.setAmount(request.getInitialDeposit().setScale(2, RoundingMode.HALF_UP));
            payment.setDate(LocalDate.now());
            payment.setNote("Initial deposit");
            payment.setRecordedBy(userId);
            payment.setEvent(event);
            // A mutable list is required: recordPayment/removePayment mutate it later.
            List<PaymentEntity> payments = new ArrayList<>();
            payments.add(payment);
            event.setPayments(payments);
        }

        event = eventRepository.save(event);
        return eventMapper.toResponse(event);
    }

    @Transactional
    public EventResponseDTO update(String id, EventRequestDTO request, String userId) {
        EventEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found: " + id));
        SecurityUtils.requireResourceAccess(event.getOrganization(), "event");

        EventTypeEntity eventType = eventTypeRepository.findById(request.getEventTypeId())
                .orElseThrow(() -> new NotFoundException("Event type not found: " + request.getEventTypeId()));
        SecurityUtils.requireResourceAccess(eventType.getOrganization(), "event type");

        event.setTitle(request.getTitle());
        event.setEventType(eventType);
        event.setClientName(request.getClientName());
        event.setClientPhone(request.getClientPhone());
        event.setClientEmail(request.getClientEmail());
        event.setVenue(request.getVenue());
        event.setScheduledDate(LocalDate.parse(request.getScheduledDate()));
        event.setScheduledTime(request.getScheduledTime());
        event.setGuestCount(request.getGuestCount());
        event.setNotes(request.getNotes());
        event.setPriceMode(request.getPriceMode());
        event.setSelectedPricingTierId(request.getSelectedPricingTierId());
        event.setCustomPrice(request.getCustomPrice());
        event.setTotalPrice(resolveTotalPrice(eventType, request));
        applyAttachedItems(event, request.getAttachedItems());
        if (request.getStatus() != null) {
            event.setStatus(EventStatus.valueOf(request.getStatus()));
        }
        event.setUpdatedBy(userId);
        event = eventRepository.save(event);
        return eventMapper.toResponse(event);
    }

    /**
     * Resolves the chargeable price from the pricing mode: the selected tier price for
     * TIER mode, or the supplied custom price for CUSTOM mode.
     */
    private BigDecimal resolveTotalPrice(EventTypeEntity eventType, EventRequestDTO request) {
        BigDecimal totalPrice;
        if ("TIER".equals(request.getPriceMode()) && request.getSelectedPricingTierId() != null) {
            totalPrice = eventType.getPricingTiers().stream()
                    .filter(t -> t.getId().equals(request.getSelectedPricingTierId()))
                    .findFirst()
                    .map(PricingTierEntity::getPrice)
                    .orElseThrow(() -> new BusinessRuleException("Selected pricing tier not found"));
        } else if ("CUSTOM".equals(request.getPriceMode()) && request.getCustomPrice() != null) {
            totalPrice = request.getCustomPrice();
        } else {
            throw new BusinessRuleException("Invalid price configuration");
        }
        return totalPrice.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Replaces the event's attached items. The collection is orphanRemoval-managed, so
     * clearing it deletes the removed rows and the added entities cascade on save.
     */
    private void applyAttachedItems(EventEntity event, List<AttachedItemRequestDTO> itemDTOs) {
        event.getAttachedItems().clear();
        if (itemDTOs == null) {
            return;
        }
        for (AttachedItemRequestDTO itemDTO : itemDTOs) {
            AttachedItemEntity item = new AttachedItemEntity();
            item.setInventoryItemId(itemDTO.getInventoryItemId());
            item.setVariantId(itemDTO.getVariantId());
            item.setQuantity(itemDTO.getQuantity());
            item.setSnapshotName(itemDTO.getSnapshotName());
            item.setSnapshotSize(itemDTO.getSnapshotSize());
            item.setSnapshotPrice(itemDTO.getSnapshotPrice());
            item.setEvent(event);
            event.getAttachedItems().add(item);
        }
    }

    @Transactional
    public void delete(String id) {
        EventEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found: " + id));
        SecurityUtils.requireResourceAccess(event.getOrganization(), "event");
        eventRepository.delete(event);
    }

    @Transactional
    public PaymentResponseDTO recordPayment(String eventId, PaymentRequestDTO request, String userId) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
        SecurityUtils.requireResourceAccess(event.getOrganization(), "event");

        PaymentEntity payment = new PaymentEntity();
        payment.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        payment.setDate(LocalDate.parse(request.getDate()));
        payment.setNote(request.getNote());
        payment.setRecordedBy(userId);
        payment.setEvent(event);
        event.getPayments().add(payment);
        eventRepository.save(event);

        return eventMapper.toPaymentResponse(payment);
    }

    @Transactional
    public void removePayment(String eventId, String paymentId) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
        SecurityUtils.requireResourceAccess(event.getOrganization(), "event");
        event.getPayments().removeIf(p -> p.getId().equals(paymentId));
        eventRepository.save(event);
    }
}
